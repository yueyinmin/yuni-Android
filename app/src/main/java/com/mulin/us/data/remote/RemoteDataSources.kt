package com.mulin.us.data.remote

/**
 * 云端能力的五个接口（对应工单 B0-6）。
 *
 * 这五个接口是"云端可替换"的全部边界：
 * - CloudBase 的实现放在 data/remote/cloudbase；
 * - 本地替身放在 data/remote/offline（没网、没环境时也能开发）；
 * - 上层（Repository / SyncCoordinator）只认接口。
 *
 * 换云端厂商时，只要新写一套实现，这五个文件不用改一个字。
 */

/** 身份认证 */
interface AuthenticationDataSource {
    val isLoggedIn: Boolean
    /** 当前登录用户的 id（云端 sub）；未登录返回 null */
    val currentUserId: String?

    suspend fun register(username: String, password: String): RemoteResult<String>
    suspend fun login(username: String, password: String): RemoteResult<String>
    suspend fun refreshToken(): RemoteResult<Unit>
    suspend fun logout(): RemoteResult<Unit>
    /** 读取当前用户资料（用于验证 token 是否真的有效） */
    suspend fun fetchMe(): RemoteResult<Map<String, String>>
}

/** 双人空间 */
interface CoupleRemoteDataSource {
    suspend fun createCouple(relationshipStartEpochDay: Long?): RemoteResult<RemoteCouple>
    suspend fun createInvite(): RemoteResult<String>
    suspend fun acceptInvite(code: String): RemoteResult<RemoteCouple>
    suspend fun getCouple(): RemoteResult<RemoteCouple?>

    /** 双端日期同步：读服务端权威日期；非 ECS 实现保持 NotConfigured */
    suspend fun getStartDate(): RemoteResult<RemoteCoupleDate> =
        RemoteResult.NotConfigured("当前数据源不支持日期同步")

    /** 写日期；返回服务端最终采纳的值（可能因为对方更新而拒绝本次写入） */
    suspend fun setStartDate(iso: String, updatedAt: Long): RemoteResult<RemoteCoupleDate> =
        RemoteResult.NotConfigured("当前数据源不支持日期同步")
}

/** 共同回忆 */
interface MemoryRemoteDataSource {
    suspend fun uploadMemory(memory: RemoteMemory): RemoteResult<RemoteMemory>
    suspend fun updateMemory(memory: RemoteMemory): RemoteResult<RemoteMemory>
    suspend fun deleteMemory(cloudId: String): RemoteResult<Unit>
    suspend fun fetchMemories(since: Long?, limit: Int = 100): RemoteResult<List<RemoteMemory>>
}

/** 同步（推送/拉取） */
interface SyncRemoteDataSource {
    suspend fun pushChanges(items: List<PushItem>): RemoteResult<PushOutcome>
    suspend fun pullChanges(domain: String, since: Long?, limit: Int = 200): RemoteResult<List<PullItem>>
    suspend fun fetchChangesSince(domain: String, since: Long): RemoteResult<List<PullItem>>
}

/** 文件存储 */
interface StorageDataSource {
    suspend fun uploadImage(localPath: String, remoteName: String): RemoteResult<UploadedFile>
    suspend fun requestTemporaryUrl(fileId: String): RemoteResult<String>
    suspend fun deleteImage(fileId: String): RemoteResult<Unit>
}

// ---------------- 传输模型（只在这里出现，不往上层泄漏） ----------------

data class RemoteCouple(
    val id: String,
    val memberAId: String,
    val memberBId: String?,
    val relationshipStartEpochDay: Long?,
    val status: String
)

/** 服务端日期同步结果；updatedAt 用于 last-write-wins 冲突判定 */
data class RemoteCoupleDate(
    val startDateIso: String?,
    val updatedAt: Long
)

data class RemoteMemory(
    val id: String? = null,
    val clientMutationId: String,
    val title: String,
    val content: String,
    val happenedAtEpochDay: Long? = null,
    val mood: String? = null,
    val serverUpdatedAt: Long = 0,
    val version: Int = 0,
    val deletedAt: Long? = null
)

data class PushItem(
    val clientMutationId: String,
    val entityType: String,
    val operation: String,
    val payloadJson: String
)

data class PushOutcome(
    /** 被云端接受的 mutationId */
    val accepted: List<String> = emptyList(),
    /** 因为版本冲突被拒绝的 mutationId → 服务端当前行 */
    val conflicts: Map<String, String> = emptyMap(),
    /** 因为权限被拒绝的 mutationId */
    val forbidden: List<String> = emptyList(),
    /** 没有任何变化的（重复提交，被幂等挡掉） */
    val unchanged: List<String> = emptyList()
)

data class PullItem(
    val entityType: String,
    val cloudId: String,
    val payloadJson: String,
    val serverUpdatedAt: Long,
    val version: Int,
    val deleted: Boolean
)

data class UploadedFile(
    val fileId: String,
    val byteSize: Long
)
