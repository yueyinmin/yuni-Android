package com.mulin.us.data.remote.ecs

import com.mulin.us.core.security.TokenVault
import com.mulin.us.data.remote.AuthenticationDataSource
import com.mulin.us.data.remote.CoupleRemoteDataSource
import com.mulin.us.data.remote.MemoryRemoteDataSource
import com.mulin.us.data.remote.PullItem
import com.mulin.us.data.remote.PushItem
import com.mulin.us.data.remote.PushOutcome
import com.mulin.us.data.remote.RemoteCouple
import com.mulin.us.data.remote.RemoteCoupleDate
import com.mulin.us.data.remote.RemoteMemory
import com.mulin.us.data.remote.RemoteResult
import com.mulin.us.data.remote.StorageDataSource
import com.mulin.us.data.remote.SyncRemoteDataSource
import com.mulin.us.data.remote.UploadedFile
import com.mulin.us.data.remote.cloudbase.CloudSessionState
import org.json.JSONArray
import org.json.JSONObject

/**
 * ECS 跳板的五个远端实现（与 cloudbase / offline 平级，接口一字不改）。
 *
 * 三个月到期处理：AppContainer 里把 ECS 分支去掉即可，本包可整体删除；
 * 数据库与 HTTP 协议都是标准件，换 CloudBase 只改 AppContainer 5 行。
 */

class EcsAuthDataSource(private val gate: EcsGate, private val vault: TokenVault) : AuthenticationDataSource {
    override val isLoggedIn: Boolean get() = vault.hasSession()
    override val currentUserId: String? get() = vault.userId()

    override suspend fun register(username: String, password: String): RemoteResult<String> =
        when (val r = gate.post("/auth/register", JSONObject().put("username", username).put("password", password), false)) {
            is RemoteResult.Success -> {
                val uid = r.data.optString("uid")
                val token = r.data.optString("token")
                if (uid.isBlank() || token.isBlank()) RemoteResult.Server(null, "注册返回缺字段")
                else {
                    vault.save(token, null, uid)
                    restoreCouple()
                    RemoteResult.Success(uid)
                }
            }
            else -> r as RemoteResult<String>
        }

    override suspend fun login(username: String, password: String): RemoteResult<String> =
        when (val r = gate.post("/auth/login", JSONObject().put("username", username).put("password", password), false)) {
            is RemoteResult.Success -> {
                val uid = r.data.optString("uid")
                val token = r.data.optString("token")
                if (uid.isBlank() || token.isBlank()) RemoteResult.Server(null, "登录返回缺字段")
                else {
                    vault.save(token, null, uid)
                    restoreCouple()
                    RemoteResult.Success(uid)
                }
            }
            else -> r as RemoteResult<String>
        }

    override suspend fun refreshToken(): RemoteResult<Unit> = RemoteResult.Success(Unit)

    override suspend fun logout(): RemoteResult<Unit> {
        vault.clear()
        CloudSessionState.coupleId = null
        vault.saveCoupleId(null)
        return RemoteResult.Success(Unit)
    }

    override suspend fun fetchMe(): RemoteResult<Map<String, String>> =
        when (val r = gate.get("/me")) {
            is RemoteResult.Success -> RemoteResult.Success(
                mapOf(
                    "uid" to r.data.optString("uid"),
                    "coupleId" to if (r.data.isNull("coupleId")) "" else r.data.optString("coupleId")
                )
            )
            else -> r as RemoteResult<Map<String, String>>
        }

    private suspend fun restoreCouple() {
        when (val r = gate.get("/me")) {
            is RemoteResult.Success -> {
                val cid = if (r.data.isNull("coupleId")) null else r.data.optString("coupleId").ifBlank { null }
                CloudSessionState.coupleId = cid
                vault.saveCoupleId(cid)
            }
            else -> Unit
        }
    }
}

class EcsCoupleRemoteDataSource(private val gate: EcsGate, private val vault: TokenVault) : CoupleRemoteDataSource {
    override suspend fun createCouple(relationshipStartEpochDay: Long?): RemoteResult<RemoteCouple> =
        when (val r = gate.post("/couple/create", JSONObject().put("relationshipStartEpochDay", relationshipStartEpochDay ?: JSONObject.NULL))) {
            is RemoteResult.Success -> {
                val cid = r.data.optString("coupleId")
                CloudSessionState.coupleId = cid
                vault.saveCoupleId(cid)
                RemoteResult.Success(r.data.toRemoteCouple())
            }
            else -> r as RemoteResult<RemoteCouple>
        }

    override suspend fun createInvite(): RemoteResult<String> =
        when (val r = gate.post("/couple/invite", JSONObject())) {
            is RemoteResult.Success -> RemoteResult.Success(r.data.optString("code"))
            else -> r as RemoteResult<String>
        }

    override suspend fun acceptInvite(code: String): RemoteResult<RemoteCouple> =
        when (val r = gate.post("/couple/redeem", JSONObject().put("code", code))) {
            is RemoteResult.Success -> {
                val cid = r.data.optString("coupleId")
                CloudSessionState.coupleId = cid
                vault.saveCoupleId(cid)
                RemoteResult.Success(r.data.toRemoteCouple())
            }
            else -> r as RemoteResult<RemoteCouple>
        }

    override suspend fun getCouple(): RemoteResult<RemoteCouple?> =
        when (val r = gate.get("/me")) {
            is RemoteResult.Success -> {
                val cid = if (r.data.isNull("coupleId")) "" else r.data.optString("coupleId")
                if (cid.isBlank()) RemoteResult.Success(null)
                else RemoteResult.Success(RemoteCouple(cid, r.data.optString("uid"), null, null, "active"))
            }
            else -> r as RemoteResult<RemoteCouple?>
        }

    override suspend fun getStartDate(): RemoteResult<RemoteCoupleDate> =
        when (val r = gate.get("/couple/date")) {
            is RemoteResult.Success -> RemoteResult.Success(r.data.toRemoteCoupleDate())
            else -> r as RemoteResult<RemoteCoupleDate>
        }

    override suspend fun setStartDate(iso: String, updatedAt: Long): RemoteResult<RemoteCoupleDate> =
        when (val r = gate.post("/couple/date", JSONObject()
            .put("startDateIso", iso)
            .put("startDateUpdatedAt", updatedAt))) {
            is RemoteResult.Success -> RemoteResult.Success(r.data.toRemoteCoupleDate())
            else -> r as RemoteResult<RemoteCoupleDate>
        }
}

class EcsMemoryRemoteDataSource(private val gate: EcsGate) : MemoryRemoteDataSource {
    override suspend fun uploadMemory(memory: RemoteMemory): RemoteResult<RemoteMemory> = RemoteResult.Success(memory)
    override suspend fun updateMemory(memory: RemoteMemory): RemoteResult<RemoteMemory> = RemoteResult.Success(memory)
    override suspend fun deleteMemory(cloudId: String): RemoteResult<Unit> = RemoteResult.Success(Unit)

    override suspend fun fetchMemories(since: Long?, limit: Int): RemoteResult<List<RemoteMemory>> =
        when (val r = gate.getArray("/sync/pull?domain=memory&limit=$limit" + if (since != null) "&since=$since" else "")) {
            is RemoteResult.Success -> RemoteResult.Success(r.data.toMemories())
            else -> r as RemoteResult<List<RemoteMemory>>
        }
}

class EcsSyncRemoteDataSource(private val gate: EcsGate) : SyncRemoteDataSource {
    override suspend fun pushChanges(items: List<PushItem>): RemoteResult<PushOutcome> {
        val arr = JSONArray()
        items.forEach {
            arr.put(JSONObject()
                .put("clientMutationId", it.clientMutationId)
                .put("entityType", it.entityType)
                .put("operation", it.operation)
                .put("payload", JSONObject(it.payloadJson)))
        }
        return when (val r = gate.post("/sync/push", JSONObject().put("items", arr))) {
            is RemoteResult.Success -> RemoteResult.Success(
                PushOutcome(
                    accepted = r.data.optJSONArray("accepted")?.toStrings().orEmpty(),
                    unchanged = r.data.optJSONArray("unchanged")?.toStrings().orEmpty(),
                    forbidden = r.data.optJSONArray("forbidden")?.toStrings().orEmpty(),
                    conflicts = emptyMap()
                )
            )
            else -> r as RemoteResult<PushOutcome>
        }
    }

    override suspend fun pullChanges(domain: String, since: Long?, limit: Int): RemoteResult<List<PullItem>> =
        when (val r = gate.getArray("/sync/pull?domain=$domain&limit=$limit" + if (since != null) "&since=$since" else "")) {
            is RemoteResult.Success -> RemoteResult.Success(r.data.toPullItems(domain))
            else -> r as RemoteResult<List<PullItem>>
        }

    override suspend fun fetchChangesSince(domain: String, since: Long): RemoteResult<List<PullItem>> =
        pullChanges(domain, since, 200)
}

/** 图片：跳板阶段不做私有桶，明确返回 NotConfigured，绝不假装成功 */
class EcsStorageDataSource : StorageDataSource {
    override suspend fun uploadImage(localPath: String, remoteName: String): RemoteResult<UploadedFile> =
        RemoteResult.NotConfigured("ECS 跳板阶段未实现图片私有存储（B3 范围）")
    override suspend fun requestTemporaryUrl(fileId: String): RemoteResult<String> =
        RemoteResult.NotConfigured("ECS 跳板阶段未实现图片私有存储")
    override suspend fun deleteImage(fileId: String): RemoteResult<Unit> =
        RemoteResult.NotConfigured("ECS 跳板阶段未实现图片私有存储")
}

// ---------- 映射工具 ----------

private fun JSONObject.toRemoteCoupleDate() = RemoteCoupleDate(
    startDateIso = if (isNull("startDateIso")) null else optString("startDateIso").ifBlank { null },
    updatedAt = optLong("startDateUpdatedAt")
)

private fun JSONObject.toRemoteCouple() = RemoteCouple(
    id = optString("coupleId"),
    memberAId = optString("memberAId"),
    // JSONObject.optString() 对 JSON null 返回字符串 "null"，不是空串。
    // 不先判 isNull 会把未绑定的空间误判成"已双人绑定"（实测踩过）。
    memberBId = if (isNull("memberBId")) null else optString("memberBId").ifBlank { null },
    relationshipStartEpochDay = null,
    status = optString("status", "active")
)

private fun JSONArray.toStrings(): List<String> = (0 until length()).map { optString(it) }

private fun JSONArray.toMemories(): List<RemoteMemory> = (0 until length()).map { i ->
    val o = getJSONObject(i)
    RemoteMemory(
        id = o.optString("id"),
        clientMutationId = o.optString("client_mutation_id"),
        title = o.optString("title"),
        content = o.optString("content"),
        happenedAtEpochDay = if (o.isNull("happened_at")) null else o.optLong("happened_at"),
        mood = o.optString("mood").ifBlank { null },
        serverUpdatedAt = o.optLong("server_updated_at"),
        version = o.optInt("version"),
        deletedAt = if (o.isNull("deleted_at")) null else o.optLong("deleted_at")
    )
}

private fun JSONArray.toPullItems(domain: String): List<PullItem> = (0 until length()).map { i ->
    val o = getJSONObject(i)
    PullItem(
        entityType = domain,
        cloudId = o.optString("id"),
        payloadJson = o.toString(),
        serverUpdatedAt = o.optLong("server_updated_at"),
        version = o.optInt("version"),
        deleted = !o.isNull("deleted_at") && o.optLong("deleted_at") > 0
    )
}
