package com.mulin.us.domain.model

/**
 * 领域模型（Domain Model）
 *
 * 和 Room 实体（data/local/entity）刻意分开：
 * - 实体是"数据库长什么样"，字段全是 String/Long，方便存；
 * - 模型是"业务上是什么"，用枚举、带语义。
 * Repository 负责两边转换。这样以后换数据库（甚至换云端）不影响上层。
 */

/** 同步状态。六个状态缺一不可，少一个就会出现"卡住不动"的数据。 */
enum class SyncStatus {
    /** 只在本地，从没上传过（私密日记默认就是这个状态，永远不上传） */
    LOCAL_ONLY,

    /** 已进待上传队列 */
    PENDING_UPLOAD,

    /** 与云端一致 */
    SYNCED,

    /** 已本地删除，等云端删除 */
    PENDING_DELETE,

    /** 云端版本比本地新，等用户决定 */
    CONFLICT,

    /** 重试耗尽，等手动重试 */
    FAILED;

    companion object {
        fun from(value: String?): SyncStatus =
            entries.firstOrNull { it.name == value } ?: LOCAL_ONLY
    }
}

/** 同步操作类型 */
enum class SyncOperation { CREATE, UPDATE, DELETE }

/** 日记可见性。PRIVATE 的日记永远不进上传队列。 */
enum class DiaryVisibility {
    PRIVATE,
    SHARED;

    companion object {
        fun from(value: String?): DiaryVisibility =
            entries.firstOrNull { it.name == value } ?: PRIVATE
    }
}

/** 本机用户资料。密码永远不在这里，密码由云端身份认证托管。 */
data class LocalUser(
    val id: String,
    val username: String,
    val nickname: String,
    val avatarLocalPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/** 共同空间 */
data class CoupleProfile(
    val id: String,
    val memberAId: String,
    val memberBId: String? = null,
    /** 恋爱开始日，存 epochDay（1970-01-01 起第几天），避免时区问题 */
    val relationshipStartEpochDay: Long? = null,
    val status: String = "active",
    val createdAt: Long = System.currentTimeMillis()
) {
    val isBound: Boolean get() = memberBId != null
}

/** 共同回忆 */
data class Memory(
    val localId: Long = 0,
    val cloudId: String? = null,
    val title: String,
    val content: String,
    val happenedAt: Long? = null,
    val mood: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY
)

/**
 * 日记。
 * visibility = PRIVATE 时 ownerId 必须等于当前用户；且 [com.mulin.us.data.repository.impl.DiaryRepositoryImpl]
 * 会强制把它的 syncStatus 固定为 LOCAL_ONLY —— 私密日记在 B0 阶段一律不上云。
 */
data class Diary(
    val localId: Long = 0,
    val cloudId: String? = null,
    val ownerId: String,
    val visibility: DiaryVisibility,
    val title: String? = null,
    val content: String,
    val entryEpochDay: Long,
    val mood: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY
) {
    val isPrivate: Boolean get() = visibility == DiaryVisibility.PRIVATE
}

/** 每日双人问答。答案由云端云函数控制揭晓，本地只缓存"已揭晓"的结果。 */
data class DailyQuestion(
    val localId: Long = 0,
    val cloudId: String? = null,
    val questionEpochDay: Long,
    val content: String,
    val myAnswer: String? = null,
    val partnerAnswer: String? = null,
    val revealed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/** 未来的信。B0 只在本地记"有这封信"，正文与解锁判定在 B3/B5 才做。 */
data class Letter(
    val localId: Long = 0,
    val cloudId: String? = null,
    val title: String,
    val unlockAt: Long,
    val senderId: String,
    val receiverId: String,
    val hasLocalDraft: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/** 愿望清单 */
data class Wish(
    val localId: Long = 0,
    val cloudId: String? = null,
    val title: String,
    val note: String? = null,
    val targetAt: Long? = null,
    val doneAt: Long? = null,
    val priority: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY
)

/** 下一次见面计划 */
data class MeetingPlan(
    val localId: Long = 0,
    val cloudId: String? = null,
    val plannedAt: Long,
    val place: String? = null,
    val note: String? = null,
    val doneAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY
)

/** 一条待同步任务（本地 Outbox 的一行） */
data class SyncTask(
    val taskId: String,
    val entityType: String,
    val entityId: Long,
    val operation: SyncOperation,
    val payload: String,
    val clientMutationId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD
)

/** 同步游标：每个"数据域"记一个最后同步位置 */
data class SyncCursor(
    val dataDomain: String,
    val lastSyncedAt: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)
