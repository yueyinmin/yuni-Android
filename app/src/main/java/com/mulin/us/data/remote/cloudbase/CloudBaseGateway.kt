package com.mulin.us.data.remote.cloudbase

import com.mulin.us.core.common.AppConfig
import com.mulin.us.core.common.SafeLog
import com.mulin.us.core.security.TokenVault
import com.mulin.us.data.remote.RemoteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * CloudBase 的 HTTP 通道。
 *
 * 所有请求都从这里走，好处是三件事只需做一次：
 * 1. 鉴权头统一加（Authorization: Bearer <access_token>）；
 * 2. 401 统一尝试刷新一次 token，再失败就转成 Unauthorized；
 * 3. 错误码统一映射成 RemoteResult（403 不重试、409 走冲突、5xx 走重试）。
 *
 * 接口地址来自 2026-11-26 核实的官方文档：
 * - 登录：POST https://{envId}.api.tcloudbasegateway.com/auth/v1/signin
 * - 注册：POST .../auth/v1/signup
 * - 刷新：POST .../auth/v1/token
 * - 当前用户：GET .../auth/v1/user/me
 * - 数据：{GET|POST|PATCH|DELETE} https://{envId}.api.tcloudbasegateway.com/v1/rdb/rest/{table}
 *
 * 注意：以上路径均为"文档存在、但本机没有真实环境未实测"的状态，见
 * 《予你 CloudBase 免费环境验证报告》里逐项标注的「未验证」。
 */
class CloudBaseGateway(private val vault: TokenVault) {

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // ---------------- 认证 ----------------

    suspend fun signIn(username: String, password: String): RemoteResult<Session> {
        if (!AppConfig.canAttemptLogin) return RemoteResult.NotConfigured("未配置 CloudBase 环境")
        val body = JSONObject().put("username", username).put("password", password)
        return when (val r = postJson(url = "${AppConfig.authBaseUrl}/signin", body = body, bearer = false)) {
            is RemoteResult.Success -> parseSession(r.data)
            else -> r as RemoteResult<Session>
        }
    }

    suspend fun signUp(username: String, password: String): RemoteResult<Session> {
        if (!AppConfig.canAttemptLogin) return RemoteResult.NotConfigured("未配置 CloudBase 环境")
        val body = JSONObject()
            .put("username", username)
            .put("password", password)
        return when (val r = postJson(url = "${AppConfig.authBaseUrl}/signup", body = body, bearer = false)) {
            is RemoteResult.Success -> parseSession(r.data)
            else -> r as RemoteResult<Session>
        }
    }

    /**
     * 用 refresh_token 换新的 access_token。
     * 文档里 refresh 走 POST /auth/v1/token，body 用 grant_type=refresh_token。
     * 这条路径本机未实测 —— 若真实环境返回 400，请按控制台实际接口调整此处（只改这一个方法）。
     */
    suspend fun refresh(): RemoteResult<Unit> {
        val refreshToken = vault.refreshToken()
            ?: return RemoteResult.Unauthorized("没有 refresh_token")
        val body = JSONObject()
            .put("grant_type", "refresh_token")
            .put("refresh_token", refreshToken)
        return when (val r = postJson(url = "${AppConfig.authBaseUrl}/token", body = body, bearer = false)) {
            is RemoteResult.Success -> {
                val access = r.data.optString("access_token")
                if (access.isBlank()) {
                    RemoteResult.Unauthorized("刷新返回空 token")
                } else {
                    vault.save(
                        accessToken = access,
                        refreshToken = r.data.optString("refresh_token").ifBlank { refreshToken },
                        userId = vault.userId() ?: r.data.optString("sub")
                    )
                    RemoteResult.Success(Unit)
                }
            }
            else -> r as RemoteResult<Unit>
        }
    }

    suspend fun fetchMe(): RemoteResult<JSONObject> {
        if (!AppConfig.cloudEnabled) return RemoteResult.NotConfigured("未配置 CloudBase 环境")
        return getJson("${AppConfig.authBaseUrl}/user/me", bearer = true)
    }

    suspend fun revoke(): RemoteResult<Unit> {
        // 登出：先本地清，再尽力通知服务端；服务端失败不影响本地已登出的事实
        val token = vault.accessToken()
        vault.clear()
        if (token == null || !AppConfig.cloudEnabled) return RemoteResult.Success(Unit)
        return when (val r = postJson("${AppConfig.authBaseUrl}/revoke", JSONObject(), bearer = false, rawBearer = token)) {
            is RemoteResult.Success -> RemoteResult.Success(Unit)
            else -> RemoteResult.Success(Unit)   // 本地已登出，服务端失败不算失败
        }
    }

    // ---------------- 数据（PostgREST） ----------------

    suspend fun restSelect(table: String, query: String): RemoteResult<JSONArray> {
        if (!AppConfig.cloudEnabled) return RemoteResult.NotConfigured("未配置 CloudBase 环境")
        val url = "${AppConfig.pgRestBaseUrl}/$table" + if (query.isBlank()) "" else "?$query"
        return when (val r = getJsonArray(url, bearer = true)) {
            is RemoteResult.Success -> r
            else -> r as RemoteResult<JSONArray>
        }
    }

    suspend fun restInsert(table: String, row: JSONObject): RemoteResult<JSONArray> {
        if (!AppConfig.cloudEnabled) return RemoteResult.NotConfigured("未配置 CloudBase 环境")
        return postJsonArray(
            url = "${AppConfig.pgRestBaseUrl}/$table",
            body = row,
            bearer = true,
            prefer = "return=representation"
        )
    }

    /** 带乐观锁的更新：条件里必须有 version=eq.{expected}，否则就是"盲改" */
    suspend fun restUpdate(table: String, filters: String, patch: JSONObject): RemoteResult<JSONArray> {
        if (!AppConfig.cloudEnabled) return RemoteResult.NotConfigured("未配置 CloudBase 环境")
        val url = "${AppConfig.pgRestBaseUrl}/$table?$filters"
        return patchJsonArray(url, patch, bearer = true, prefer = "return=representation")
    }

    suspend fun restDelete(table: String, filters: String): RemoteResult<Unit> {
        if (!AppConfig.cloudEnabled) return RemoteResult.NotConfigured("未配置 CloudBase 环境")
        return when (val r = deleteRequest("${AppConfig.pgRestBaseUrl}/$table?$filters", bearer = true)) {
            is RemoteResult.Success -> RemoteResult.Success(Unit)
            else -> r as RemoteResult<Unit>
        }
    }

    // ---------------- 底层 HTTP ----------------

    private fun parseSession(obj: JSONObject): RemoteResult<Session> {
        val access = obj.optString("access_token")
        val sub = obj.optString("sub")
        if (access.isBlank() || sub.isBlank()) {
            return RemoteResult.Server(null, "登录返回缺少 access_token 或 sub")
        }
        vault.save(access, obj.optString("refresh_token"), sub)
        SafeLog.i("CloudBase", "sign-in ok uid=${SafeLog.mask(sub)}")
        return RemoteResult.Success(
            Session(
                userId = sub,
                accessToken = access,
                expiresInSeconds = obj.optLong("expires_in", 7200L)
            )
        )
    }

    private suspend fun getJson(url: String, bearer: Boolean): RemoteResult<JSONObject> =
        requestWithRetry(Request.Builder().url(url).get().build(), bearer).map { body ->
            JSONObject(body ?: "{}")
        }

    private suspend fun getJsonArray(url: String, bearer: Boolean): RemoteResult<JSONArray> =
        requestWithRetry(
            Request.Builder().url(url).get().header("Accept", "application/json").build(),
            bearer
        ).map { body -> JSONArray(body ?: "[]") }

    private suspend fun postJson(
        url: String,
        body: JSONObject,
        bearer: Boolean,
        rawBearer: String? = null
    ): RemoteResult<JSONObject> {
        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody(jsonMedia))
            .header("Content-Type", "application/json")
            .build()
        return requestWithRetry(request, bearer, rawBearer).map { text ->
            JSONObject(text ?: "{}")
        }
    }

    private suspend fun postJsonArray(
        url: String,
        body: JSONObject,
        bearer: Boolean,
        prefer: String
    ): RemoteResult<JSONArray> {
        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody(jsonMedia))
            .header("Content-Type", "application/json")
            .header("Prefer", prefer)
            .header("Accept", "application/json")
            .build()
        return requestWithRetry(request, bearer).map { text -> JSONArray(text ?: "[]") }
    }

    private suspend fun patchJsonArray(
        url: String,
        body: JSONObject,
        bearer: Boolean,
        prefer: String
    ): RemoteResult<JSONArray> {
        val request = Request.Builder()
            .url(url)
            .patch(body.toString().toRequestBody(jsonMedia))
            .header("Content-Type", "application/json")
            .header("Prefer", prefer)
            .header("Accept", "application/json")
            .build()
        return requestWithRetry(request, bearer).map { text -> JSONArray(text ?: "[]") }
    }

    private suspend fun deleteRequest(url: String, bearer: Boolean): RemoteResult<String> =
        requestWithRetry(Request.Builder().url(url).delete().build(), bearer)

    /**
     * 执行一次请求；401 时刷新 token 并重试一次（只重试一次，避免死循环）。
     */
    private suspend fun requestWithRetry(
        request: Request,
        bearer: Boolean,
        rawBearer: String? = null
    ): RemoteResult<String> {
        val token = rawBearer ?: if (bearer) vault.accessToken() else null
        val first = request.newBuilder()
            .header("Authorization", "Bearer ${token ?: ""}")
            .build()
        var result = execute(first)
        if (result is RemoteResult.Unauthorized && bearer && rawBearer == null) {
            val refreshed = refresh()
            if (refreshed is RemoteResult.Success) {
                val newToken = vault.accessToken()
                val second = request.newBuilder()
                    .header("Authorization", "Bearer ${newToken ?: ""}")
                    .build()
                result = execute(second)
            }
        }
        return result
    }

    private suspend fun execute(request: Request): RemoteResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(request).await()
            response.use { res ->
                val text = res.body?.string().orEmpty()
                when {
                    res.isSuccessful -> RemoteResult.Success(text)
                    res.code == 401 -> RemoteResult.Unauthorized(extractMessage(text))
                    res.code == 403 -> RemoteResult.Forbidden(extractMessage(text))
                    res.code == 409 -> RemoteResult.Conflict(extractMessage(text), text)
                    res.code in 400..499 -> RemoteResult.BadRequest(extractCode(text), extractMessage(text))
                    else -> RemoteResult.Server(extractCode(text), extractMessage(text))
                }
            }
        } catch (e: IOException) {
            RemoteResult.Network(e.message ?: "网络异常")
        } catch (e: Exception) {
            RemoteResult.Server(null, e.message ?: "未知异常")
        }
    }

    private fun extractCode(body: String): String? = runCatching {
        JSONObject(body).optString("code").ifBlank { null }
    }.getOrNull()

    private fun extractMessage(body: String): String = runCatching {
        val obj = JSONObject(body)
        obj.optString("message").ifBlank { obj.optString("error_description") }
            .ifBlank { body.take(200) }
    }.getOrDefault(body.take(200))

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (cont.isActive) cont.resume(response) else response.close()
            }
        })
        cont.invokeOnCancellation { runCatching { cancel() } }
    }

    data class Session(
        val userId: String,
        val accessToken: String,
        val expiresInSeconds: Long
    )
}
