package com.mulin.us.data.remote.cloudbase

import com.mulin.us.data.auth.TokenVault
import com.mulin.us.data.remote.AuthenticationDataSource
import com.mulin.us.data.remote.CoupleRemoteDataSource
import com.mulin.us.data.remote.MemoryRemoteDataSource
import com.mulin.us.data.remote.PullItem
import com.mulin.us.data.remote.PushItem
import com.mulin.us.data.remote.PushOutcome
import com.mulin.us.data.remote.RemoteCouple
import com.mulin.us.data.remote.RemoteMemory
import com.mulin.us.data.remote.RemoteResult
import com.mulin.us.data.remote.StorageDataSource
import com.mulin.us.data.remote.SyncRemoteDataSource
import com.mulin.us.data.remote.UploadedFile
import org.json.JSONArray
import org.json.JSONObject

/**
 * 云端会话的共享状态：当前 coupleId。
 *
 * 注意（很重要）：客户端持有 coupleId **不代表**获得了权限。
 * 服务端每一行都还有 RLS 策略 `is_couple_member(couple_id)` 在把关，
 * 客户端把 coupleId 改成别人的，只会得到空结果或 403。
 * 这里存它只是为了拼 URL 和写 INSERT 的 couple_id 列。
 */
object CloudSessionState {
    @Volatile
    var coupleId: String? = null
}

// ============================================================
// 1. 身份认证
// ============================================================
class CloudBaseAuthDataSource(
    private val gateway: CloudBaseGateway,
    private val vault: TokenVault
) : AuthenticationDataSource {

    override val isLoggedIn: Boolean get() = vault.hasSession()
    override val currentUserId: String? get() = vault.userId()

    override suspend fun register(username: String, password: String): RemoteResult<String> =
        gateway.signUp(username, password).map { it.userId }

    override suspend fun login(username: String, password: String): RemoteResult<String> =
        gateway.signIn(username, password).map { it.userId }

    override suspend fun refreshToken(): RemoteResult<Unit> = gateway.refresh()

    override suspend fun logout(): RemoteResult<Unit> = gateway.revoke()

    override suspend fun fetchMe(): RemoteResult<Map<String, String>> =
        gateway.fetchMe().map { obj ->
            buildMap {
                obj.keys().forEach { key -> put(key, obj.optString(key)) }
            }
        }
}

// ============================================================
// 2. 双人空间
// ============================================================
class CloudBaseCoupleRemoteDataSource(
    private val gateway: CloudBaseGateway
) : CoupleRemoteDataSource {

    override suspend fun createCouple(relationshipStartEpochDay: Long?): RemoteResult<RemoteCouple> {
        // member_a 不给值，交给数据库默认值 auth.uid()。
        // 为什么不在客户端传：传了也白传——客户端可以传任何人，服务端必须用 auth.uid()。
        val row = JSONObject()
            .put("relationship_start_epoch_day", relationshipStartEpochDay ?: JSONObject.NULL)
            .put("status", "active")
        val result = gateway.restInsert("couple", row)
        return if (result is RemoteResult.Success) {
            firstCouple(result.data)
        } else {
            result as RemoteResult<RemoteCouple>
        }
    }

    override suspend fun createInvite(): RemoteResult<String> {
        // 邀请码必须由云函数生成（要存哈希、要防并发核销），REST 不能直接写这张表
        return RemoteResult.NotConfigured("邀请码需要云函数 couple.issueInvite，B0 未部署")
    }

    override suspend fun acceptInvite(code: String): RemoteResult<RemoteCouple> =
        RemoteResult.NotConfigured("核销邀请码需要云函数 couple.redeem，B0 未部署")

    override suspend fun getCouple(): RemoteResult<RemoteCouple?> {
        // PostgREST 的 or 过滤器：member_a=eq.uid 或 member_b=eq.uid
        val uid = TokenHolder.uid ?: return RemoteResult.Unauthorized("未登录")
        val query = "select=*&or=(member_a.eq.$uid,member_b.eq.$uid)&status=eq.active&limit=1"
        return when (val r = gateway.restSelect("couple", query)) {
            is RemoteResult.Success -> {
                if (r.data.length() == 0) {
                    CloudSessionState.coupleId = null
                    RemoteResult.Success(null)
                } else firstCouple(r.data)
            }
            else -> r as RemoteResult<RemoteCouple?>
        }
    }

    private fun firstCouple(arr: JSONArray): RemoteResult<RemoteCouple> {
        if (arr.length() == 0) return RemoteResult.Server(null, "创建空间未返回数据")
        val o = arr.getJSONObject(0)
        val couple = RemoteCouple(
            id = o.optString("id"),
            memberAId = o.optString("member_a"),
            memberBId = o.optString("member_b").ifBlank { null },
            relationshipStartEpochDay = if (o.isNull("relationship_start_epoch_day")) null
            else o.optLong("relationship_start_epoch_day"),
            status = o.optString("status", "active")
        )
        CloudSessionState.coupleId = couple.id
        return RemoteResult.Success(couple)
    }
}

/** 极小化的只读 token 持有者，供拼查询条件用（不参与任何权限判断） */
object TokenHolder {
    @Volatile
    var uid: String? = null
}

// ============================================================
// 3. 共同回忆
// ============================================================
class CloudBaseMemoryRemoteDataSource(
    private val gateway: CloudBaseGateway
) : MemoryRemoteDataSource {

    override suspend fun uploadMemory(memory: RemoteMemory): RemoteResult<RemoteMemory> {
        val coupleId = CloudSessionState.coupleId
            ?: return RemoteResult.NotConfigured("还没有共同空间，无法上传")
        val row = JSONObject()
            .put("couple_id", coupleId)
            .put("client_mutation_id", memory.clientMutationId)
            .put("title", memory.title)
            .put("content", memory.content)
            .put("happened_at", memory.happenedAtEpochDay ?: JSONObject.NULL)
            .put("mood", memory.mood ?: JSONObject.NULL)
            .put("server_updated_at", System.currentTimeMillis())
        return when (val r = gateway.restInsert("memory", row)) {
            is RemoteResult.Success -> parseMemory(r.data)
            is RemoteResult.Conflict -> RemoteResult.Success(memory)  // 幂等：已存在即视为成功
            else -> r as RemoteResult<RemoteMemory>
        }
    }

    override suspend fun updateMemory(memory: RemoteMemory): RemoteResult<RemoteMemory> {
        val cloudId = memory.id ?: return RemoteResult.BadRequest(null, "缺少云端 id")
        val patch = JSONObject()
            .put("title", memory.title)
            .put("content", memory.content)
            .put("server_updated_at", System.currentTimeMillis())
        // 乐观锁：只有版本一致才更新，否则 PostgREST 返回 0 行
        val filters = "id=eq.$cloudId&version=eq.${memory.version}"
        return when (val r = gateway.restUpdate("memory", filters, patch)) {
            is RemoteResult.Success -> {
                if (r.data.length() == 0) RemoteResult.Conflict("版本不一致", null)
                else parseMemory(r.data)
            }
            else -> r as RemoteResult<RemoteMemory>
        }
    }

    override suspend fun deleteMemory(cloudId: String): RemoteResult<Unit> =
        when (val r = gateway.restUpdate(
            "memory",
            "id=eq.$cloudId",
            JSONObject().put("deleted_at", System.currentTimeMillis())
        )) {
            is RemoteResult.Success -> RemoteResult.Success(Unit)
            else -> r as RemoteResult<Unit>
        }

    override suspend fun fetchMemories(since: Long?, limit: Int): RemoteResult<List<RemoteMemory>> {
        val coupleId = CloudSessionState.coupleId
            ?: return RemoteResult.Success(emptyList())
        val query = buildString {
            append("select=*&couple_id=eq.").append(coupleId)
            append("&deleted_at=is.null")
            if (since != null && since > 0) append("&server_updated_at=gt.").append(since)
            append("&order=server_updated_at.asc&limit=").append(limit)
        }
        return when (val r = gateway.restSelect("memory", query)) {
            is RemoteResult.Success -> RemoteResult.Success(
                (0 until r.data.length()).map { i -> toRemoteMemory(r.data.getJSONObject(i)) }
            )
            else -> r as RemoteResult<List<RemoteMemory>>
        }
    }

    private fun parseMemory(arr: JSONArray): RemoteResult<RemoteMemory> {
        if (arr.length() == 0) return RemoteResult.Server(null, "写入未返回数据")
        return RemoteResult.Success(toRemoteMemory(arr.getJSONObject(0)))
    }

    private fun toRemoteMemory(o: JSONObject) = RemoteMemory(
        id = o.optString("id").ifBlank { null },
        clientMutationId = o.optString("client_mutation_id"),
        title = o.optString("title"),
        content = o.optString("content"),
        happenedAtEpochDay = if (o.isNull("happened_at")) null else o.optLong("happened_at"),
        mood = o.optString("mood").ifBlank { null },
        serverUpdatedAt = o.optLong("server_updated_at"),
        version = o.optInt("version", 1),
        deletedAt = if (o.isNull("deleted_at")) null else o.optLong("deleted_at")
    )
}

// ============================================================
// 4. 同步（推送/拉取）
// ============================================================
class CloudBaseSyncRemoteDataSource(
    private val gateway: CloudBaseGateway
) : SyncRemoteDataSource {

    override suspend fun pushChanges(items: List<PushItem>): RemoteResult<PushOutcome> {
        if (items.isEmpty()) return RemoteResult.Success(PushOutcome())
        val accepted = mutableListOf<String>()
        val conflicts = mutableMapOf<String, String>()
        val forbidden = mutableListOf<String>()
        val unchanged = mutableListOf<String>()

        // B0 阶段顺序推送：一次一条，失败即归类，不做批量事务。
        // 批量要走云函数，B0 不引入。
        for (item in items) {
            val table = tableOf(item.entityType)
            if (table == null) {
                forbidden += item.clientMutationId
                continue
            }
            val row = JSONObject(item.payloadJson)
            val coupleId = CloudSessionState.coupleId
            if (coupleId != null && !row.has("couple_id")) row.put("couple_id", coupleId)
            row.put("client_mutation_id", item.clientMutationId)
            row.put("server_updated_at", System.currentTimeMillis())
            when (val r = gateway.restInsert(table, row)) {
                is RemoteResult.Success -> accepted += item.clientMutationId
                is RemoteResult.Conflict -> unchanged += item.clientMutationId
                is RemoteResult.Forbidden, is RemoteResult.Unauthorized -> forbidden += item.clientMutationId
                else -> return r as RemoteResult<PushOutcome>
            }
        }
        return RemoteResult.Success(PushOutcome(accepted, conflicts, forbidden, unchanged))
    }

    override suspend fun pullChanges(domain: String, since: Long?, limit: Int): RemoteResult<List<PullItem>> {
        val table = tableOf(domain) ?: return RemoteResult.BadRequest(null, "未知数据域 $domain")
        val coupleId = CloudSessionState.coupleId
            ?: return RemoteResult.Success(emptyList())
        val query = buildString {
            append("select=*&couple_id=eq.").append(coupleId)
            if (since != null && since > 0) append("&server_updated_at=gt.").append(since)
            append("&order=server_updated_at.asc&limit=").append(limit)
        }
        return when (val r = gateway.restSelect(table, query)) {
            is RemoteResult.Success -> RemoteResult.Success(toPullItems(domain, r.data))
            else -> r as RemoteResult<List<PullItem>>
        }
    }

    override suspend fun fetchChangesSince(domain: String, since: Long): RemoteResult<List<PullItem>> =
        pullChanges(domain, since, 200)

    private fun toPullItems(domain: String, arr: JSONArray): List<PullItem> =
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            PullItem(
                entityType = domain,
                cloudId = o.optString("id"),
                payloadJson = o.toString(),
                serverUpdatedAt = o.optLong("server_updated_at"),
                version = o.optInt("version", 1),
                deleted = !o.isNull("deleted_at")
            )
        }

    private fun tableOf(entityType: String): String? = when (entityType) {
        "memory" -> "memory"
        "diary" -> "diary"
        "wish" -> "wish"
        "meeting_plan", "meeting" -> "meeting_plan"
        else -> null
    }
}

// ============================================================
// 5. 文件存储
// ============================================================
class CloudBaseStorageDataSource(
    private val gateway: CloudBaseGateway
) : StorageDataSource {

    /**
     * 为什么这里是 NotConfigured 而不是"假装上传成功"：
     * CloudBase 云存储的上传接口（HTTP）本机没有环境、也没拿到可验证的文档定义，
     * 按工单要求"无法验证的必须标记待验证，不得给出通过结论"，
     * 所以这里返回明确的未配置，绝不伪造成功。
     */
    override suspend fun uploadImage(localPath: String, remoteName: String): RemoteResult<UploadedFile> =
        RemoteResult.NotConfigured("云存储上传接口未核实（见验证报告 S3 项），B0 不上传")

    override suspend fun requestTemporaryUrl(fileId: String): RemoteResult<String> =
        RemoteResult.NotConfigured("私有文件临时链接接口未核实，B0 不启用")

    override suspend fun deleteImage(fileId: String): RemoteResult<Unit> =
        RemoteResult.NotConfigured("云存储删除接口未核实，B0 不启用")
}
