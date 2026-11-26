package com.mulin.us.data.remote.offline

import android.content.Context
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
import java.io.File
import java.util.UUID

/**
 * 本地替身（Offline / Fake）实现。
 *
 * 用途：没有 CloudBase 环境时，整个 App 依然能跑、能写、能同步（同步到内存）。
 * 这不是"假装有云端"，而是为了让同步逻辑、冲突逻辑、Worker 调度在 0 元前提下就能开发。
 *
 * 严格区分两种东西：
 * - 这五个类：测试替身，明确标记 Offline/Fake，数据只在内存里，重启 App 就没了；
 * - cloudbase 包下的类：真实网络实现，没配置环境时返回 NotConfigured，绝不伪造成功。
 */

class OfflineAuthDataSource : AuthenticationDataSource {
    override val isLoggedIn: Boolean = false
    override val currentUserId: String? = null

    override suspend fun register(username: String, password: String): RemoteResult<String> =
        RemoteResult.NotConfigured("本地模式：未配置 CloudBase 环境，无法注册云端账号")

    override suspend fun login(username: String, password: String): RemoteResult<String> =
        RemoteResult.NotConfigured("本地模式：未配置 CloudBase 环境，无法登录云端账号")

    override suspend fun refreshToken(): RemoteResult<Unit> =
        RemoteResult.NotConfigured("本地模式无 token")

    override suspend fun logout(): RemoteResult<Unit> = RemoteResult.Success(Unit)

    override suspend fun fetchMe(): RemoteResult<Map<String, String>> =
        RemoteResult.NotConfigured("本地模式无云端用户")
}

class OfflineCoupleRemoteDataSource : CoupleRemoteDataSource {
    override suspend fun createCouple(relationshipStartEpochDay: Long?): RemoteResult<RemoteCouple> =
        RemoteResult.NotConfigured("本地模式：共同空间只存在本机")

    override suspend fun createInvite(): RemoteResult<String> =
        RemoteResult.NotConfigured("本地模式无法生成邀请码")

    override suspend fun acceptInvite(code: String): RemoteResult<RemoteCouple> =
        RemoteResult.NotConfigured("本地模式无法接受邀请")

    override suspend fun getCouple(): RemoteResult<RemoteCouple?> = RemoteResult.Success(null)
}

class OfflineMemoryRemoteDataSource : MemoryRemoteDataSource {
    private val store = linkedMapOf<String, RemoteMemory>()

    override suspend fun uploadMemory(memory: RemoteMemory): RemoteResult<RemoteMemory> {
        val id = memory.id ?: UUID.randomUUID().toString()
        val saved = memory.copy(id = id, serverUpdatedAt = System.currentTimeMillis(), version = 1)
        store[id] = saved
        return RemoteResult.Success(saved)
    }

    override suspend fun updateMemory(memory: RemoteMemory): RemoteResult<RemoteMemory> {
        val id = memory.id ?: return RemoteResult.BadRequest(null, "缺少 id")
        val existing = store[id] ?: return RemoteResult.BadRequest(null, "记录不存在")
        if (existing.version != memory.version) return RemoteResult.Conflict("版本不一致", null)
        val saved = memory.copy(version = existing.version + 1, serverUpdatedAt = System.currentTimeMillis())
        store[id] = saved
        return RemoteResult.Success(saved)
    }

    override suspend fun deleteMemory(cloudId: String): RemoteResult<Unit> {
        store.remove(cloudId)
        return RemoteResult.Success(Unit)
    }

    override suspend fun fetchMemories(since: Long?, limit: Int): RemoteResult<List<RemoteMemory>> =
        RemoteResult.Success(
            store.values
                .filter { since == null || it.serverUpdatedAt > since }
                .sortedBy { it.serverUpdatedAt }
                .take(limit)
        )
}

/**
 * 同步替身：把推送上来的 payload 收进内存，再原样供拉取。
 * 作用 —— 让 SyncWorker → push → 标 SYNCED 这条链路能在没有云端的情况下被完整测到。
 */
class FakeSyncRemoteDataSource : SyncRemoteDataSource {

    private val accepted = linkedMapOf<String, MutableList<PullItem>>()

    val pushedCount: Int get() = accepted.values.sumOf { it.size }

    override suspend fun pushChanges(items: List<PushItem>): RemoteResult<PushOutcome> {
        val ok = mutableListOf<String>()
        val unchanged = mutableListOf<String>()
        items.forEach { item ->
            val bucket = accepted.getOrPut(item.entityType) { mutableListOf() }
            if (bucket.any { it.payloadJson.contains(item.clientMutationId) }) {
                // 幂等：同一个 clientMutationId 第二次推送直接当成功，不产生新行
                unchanged += item.clientMutationId
            } else {
                bucket += PullItem(
                    entityType = item.entityType,
                    cloudId = "fake-" + item.clientMutationId,
                    payloadJson = item.payloadJson,
                    serverUpdatedAt = System.currentTimeMillis(),
                    version = 1,
                    deleted = item.operation == "DELETE"
                )
                ok += item.clientMutationId
            }
        }
        return RemoteResult.Success(PushOutcome(accepted = ok, unchanged = unchanged))
    }

    override suspend fun pullChanges(domain: String, since: Long?, limit: Int): RemoteResult<List<PullItem>> {
        val bucket = accepted[domain].orEmpty()
        return RemoteResult.Success(
            bucket.filter { since == null || it.serverUpdatedAt > since }.take(limit)
        )
    }

    override suspend fun fetchChangesSince(domain: String, since: Long): RemoteResult<List<PullItem>> =
        pullChanges(domain, since, 200)
}

/** 本地文件存储：只落在应用私有目录，别的 App 读不到，也不需要任何存储权限 */
class LocalStorageDataSource(private val context: Context) : StorageDataSource {

    override suspend fun uploadImage(localPath: String, remoteName: String): RemoteResult<UploadedFile> {
        val src = File(localPath)
        if (!src.exists()) return RemoteResult.BadRequest(null, "源文件不存在")
        val dir = File(context.filesDir, "images").apply { mkdirs() }
        val dest = File(dir, remoteName)
        src.copyTo(dest, overwrite = true)
        return RemoteResult.Success(UploadedFile(fileId = "local://${dest.absolutePath}", byteSize = dest.length()))
    }

    /** 本地模式下"临时链接"就是文件本身，没有有效期概念（不涉及公网，所以不存在泄露） */
    override suspend fun requestTemporaryUrl(fileId: String): RemoteResult<String> {
        val path = fileId.removePrefix("local://")
        val f = File(path)
        return if (f.exists()) RemoteResult.Success(path)
        else RemoteResult.BadRequest(null, "文件不存在")
    }

    override suspend fun deleteImage(fileId: String): RemoteResult<Unit> {
        val f = File(fileId.removePrefix("local://"))
        if (f.exists()) f.delete()
        return RemoteResult.Success(Unit)
    }
}
