package com.mulin.us.data.remote.ecs

import com.mulin.us.core.common.SafeLog
import com.mulin.us.core.security.TokenVault
import com.mulin.us.data.remote.cloudbase.CloudSessionState
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * ECS 跳板网关（三个月临时方案，到期整体可丢弃）。
 *
 * 设计约束（与 A 阶段决策一致）：
 * - 不依赖任何阿里云特有服务：服务端是标准 PostgreSQL + HTTP，换机器只要改 baseUrl；
 * - 所有请求经此一处：鉴权头、coupleId 头、错误码映射只做一次；
 * - 客户端零改动：五个 DataSource 实现只调这里，换云端仍只改 AppContainer 几行。
 */
class EcsGate(
    private val baseUrl: String,
    private val vault: TokenVault
) {
    fun baseUrlForImage(): String = baseUrl

    private val json = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /** coupleId 从持久化 TokenVault 读，内存态仅作镜像；不在这里发 /me，避免嵌套请求 */
    private fun currentCoupleId(): String? =
        vault.coupleId() ?: CloudSessionState.coupleId

    suspend fun post(path: String, body: JSONObject, withAuth: Boolean = true): RemoteResult<JSONObject> {
        if (baseUrl.isBlank()) return RemoteResult.NotConfigured("未配置云端地址")
        return call("POST", path, body.toString(), withAuth)
    }

    suspend fun get(path: String, withAuth: Boolean = true): RemoteResult<JSONObject> {
        if (baseUrl.isBlank()) return RemoteResult.NotConfigured("未配置云端地址")
        return call("GET", path, null, withAuth)
    }

    suspend fun getArray(path: String): RemoteResult<JSONArray> {
        if (baseUrl.isBlank()) return RemoteResult.NotConfigured("未配置云端地址")
        return getArrayRaw(path)
    }

    private suspend fun getArrayRaw(path: String): RemoteResult<JSONArray> = withContext(Dispatchers.IO) {
        val url = baseUrl.trimEnd('/') + path
        val req = Request.Builder().url(url)
            .apply {
                vault.accessToken()?.let { header("Authorization", "Bearer $it") }
                currentCoupleId()?.let { header("X-Couple-Id", it) }
            }
            .get().build()
        try {
            val resp = suspendCancellableCoroutine<Pair<Int, String>> { cont ->
                client.newCall(req).enqueue(object : Callback {
                    override fun onFailure(c: Call, e: IOException) = cont.resume(0 to e.message.orEmpty())
                    override fun onResponse(c: Call, r: okhttp3.Response) = r.use {
                        cont.resume(it.code to it.body?.string().orEmpty())
                    }
                })
            }
            val (code, text) = resp
            when {
                code == 0 -> RemoteResult.Network(text)
                code in 200..299 -> RemoteResult.Success(JSONArray(text))
                code == 401 -> RemoteResult.Unauthorized(text)
                code == 403 -> RemoteResult.Forbidden(text)
                code == 409 -> RemoteResult.Conflict(text, null)
                code in 400..499 -> RemoteResult.BadRequest(code.toString(), text)
                else -> RemoteResult.Server(code.toString(), text)
            }
        } catch (e: Exception) {
            RemoteResult.Network(e.message.orEmpty())
        }
    }

    private suspend fun call(method: String, path: String, payload: String?, withAuth: Boolean): RemoteResult<JSONObject> =
        withContext(Dispatchers.IO) {
            val url = baseUrl.trimEnd('/') + path
            val body = payload?.toRequestBody(json)
            val req = Request.Builder().url(url).apply {
                if (withAuth) {
                    vault.accessToken()?.let { header("Authorization", "Bearer $it") }
                    currentCoupleId()?.let { header("X-Couple-Id", it) }
                }
            }.method(method, body).build()
            try {
                val resp = suspendCancellableCoroutine<Pair<Int, String>> { cont ->
                    client.newCall(req).enqueue(object : Callback {
                        override fun onFailure(c: Call, e: IOException) = cont.resume(0 to e.message.orEmpty())
                        override fun onResponse(c: Call, r: okhttp3.Response) = r.use {
                            cont.resume(it.code to it.body?.string().orEmpty())
                        }
                    })
                }
                val (code, text) = resp
                SafeLog.d("Ecs", "$method $path -> $code")
                val obj = runCatching { JSONObject(text) }.getOrDefault(JSONObject())
                when {
                    code == 0 -> RemoteResult.Network(text)
                    code in 200..299 -> RemoteResult.Success(obj)
                    code == 401 -> RemoteResult.Unauthorized(obj.optString("error", text))
                    code == 403 -> RemoteResult.Forbidden(obj.optString("error", text))
                    code == 409 -> RemoteResult.Conflict(obj.optString("error", text), null)
                    code in 400..499 -> RemoteResult.BadRequest(code.toString(), obj.optString("error", text))
                    else -> RemoteResult.Server(code.toString(), obj.optString("error", text))
                }
            } catch (e: Exception) {
                RemoteResult.Network(e.message.orEmpty())
            }
        }
}
