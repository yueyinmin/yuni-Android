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
    /** 工单 V3.1：恋爱开始日 ISO 字符串（权威），优先于 epochDay */
    val startDateIso: String? = null,
    val status: String = "active",
    val createdAt: Long = System.currentTimeMillis()
) {
    val isBound: Boolean get() = memberBId != null

    /** 统一读取开始日：优先 ISO，兼容旧 epochDay */
    val startDate: java.time.LocalDate?
        get() = startDateIso?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
            ?: relationshipStartEpochDay?.let { java.time.LocalDate.ofEpochDay(it) }
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
    /**
     * 所属双人空间。null = 历史数据或离线创建、还没拿到空间。
     * 只有 SHARED 且 coupleId 非空的数据才允许进上传队列。
     */
    val coupleId: String? = null,
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
    val coupleId: String? = null,
    val questionEpochDay: Long,
    val content: String,
    val myAnswer: String? = null,
    /** 未揭晓时 DAO 保证这里是 null，不只是 UI 不显示 */
    val partnerAnswer: String? = null,
    val revealed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = 0,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
    val serverVersion: Int = 0
)

/** 信件的生命周期状态。DRAFT 才有本地草稿正文。 */
enum class LetterStatus {
    DRAFT, LOCKED, UNLOCKED, CANCELLED;

    companion object {
        fun from(value: String?): LetterStatus =
            entries.firstOrNull { it.name == value } ?: DRAFT
    }
}

/**
 * 未来的信。B0 只在本地记"有这封信"，正文与解锁判定在 B3/B5 才做。
 *
 * [localDraftContent] 是本机草稿，**任何情况下都不进 Outbox、不上云**。
 */
data class Letter(
    val localId: Long = 0,
    val cloudId: String? = null,
    val coupleId: String? = null,
    val title: String,
    val unlockAt: Long,
    val senderId: String,
    val receiverId: String,
    val localDraftContent: String? = null,
    val status: LetterStatus = LetterStatus.DRAFT,
    val hasLocalDraft: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 悄悄话（B6 功能，B0.1 只补数据模型与表，不实现收发）。
 * receiverId = null 表示"空间里的另一个人"，离线时不必知道对方 userId。
 */
data class Message(
    val localId: Long = 0,
    val cloudId: String? = null,
    val coupleId: String? = null,
    val senderId: String,
    val receiverId: String? = null,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val readAt: Long? = null,
    val deletedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
    val serverVersion: Int = 0
)

/** 回忆照片（4.8/4.9）：本地始终有副本，云端 fileId 可空 */
data class MemoryPhoto(
    val memoryCloudId: String,
    val localPath: String,
    val cloudFileId: String? = null,
    val byteSize: Long = 0,
    val synced: Boolean = false
)

/** 回忆评论（5.6） */
data class Comment(
    val id: String,
    val memoryCloudId: String,
    val authorId: String,
    val content: String,
    val mine: Boolean = false,
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
    /** 本地最后修改时间；勾选完成也要更新，否则同步判断不出谁更新 */
    val updatedAt: Long = createdAt,
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
