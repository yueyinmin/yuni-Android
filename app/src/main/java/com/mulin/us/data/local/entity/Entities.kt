package com.mulin.us.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room 实体（本地数据库的"表结构"）。
 *
 * 三个统一约定：
 * 1. 所有时间用 Long（epoch millis），不用 Date/Instant，省掉类型转换器；
 * 2. 所有枚举用 String 存（SYNCED / PRIVATE ...），同样省掉转换器，且数据库里肉眼可读；
 * 3. 所有软删除用 deletedAt: Long?，null = 没删。
 *
 * 本地表比云端表多几列（syncStatus 等），因为本地还要记"这一端还没交的作业"。
 */

/** 本机用户。id 用云端返回的 sub；未登录时是本地生成的 "local-<uuid>"。 */
@Entity(tableName = "local_user")
data class LocalUserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val nickname: String,
    val avatarLocalPath: String? = null,
    val createdAt: Long
)

/** 共同空间。全 App 最多一条（两人只有一个空间）。 */
@Entity(tableName = "couple_profile")
data class CoupleEntity(
    @PrimaryKey val id: String,
    val memberAId: String,
    val memberBId: String? = null,
    val relationshipStartEpochDay: Long? = null,
    /** 工单 V3.1 C3.5：恋爱开始日的权威存储（ISO "YYYY-MM-DD"，跨时区不偏移） */
    val startDateIso: String? = null,
    /** 最后修改时间，双端同步冲突判定用；迁移时旧数据为 0 */
    @ColumnInfo(defaultValue = "0") val startDateUpdatedAt: Long = 0,
    val status: String = "active",
    val createdAt: Long
)

/** 共同回忆 */
@Entity(
    tableName = "memory",
    indices = [Index("createdAt"), Index("syncStatus")]
)
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val cloudId: String? = null,
    val title: String,
    val content: String,
    val happenedAt: Long? = null,
    val mood: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncStatus: String = "LOCAL_ONLY",
    /** 最近一次改动的幂等键；空 = 还没进过队列 */
    val clientMutationId: String? = null,
    /** 云端版本号快照，用于乐观锁；0 = 未上传过 */
    val serverVersion: Int = 0
)

/**
 * 回忆 ↔ 本地图片 的关联。
 * 为什么单独一张表而不是在 Memory 里存一个 JSON：
 * 图片要独立删除、独立上传、独立清理孤儿文件，塞进一条字段里以后没法查。
 */
@Entity(
    tableName = "memory_photo",
    indices = [Index("memoryLocalId"), Index("cloudFileId")]
)
data class MemoryPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memoryLocalId: Long,
    /** 本地压缩后的文件绝对路径；文件不存在时 UI 显示占位 */
    val localPath: String,
    /** 缩略图本地路径 */
    val thumbLocalPath: String? = null,
    /** 云端 fileID，null = 还没上传 */
    val cloudFileId: String? = null,
    val byteSize: Long = 0,
    val createdAt: Long
)

/** 日记（私密 / 共同） */
@Entity(
    tableName = "diary",
    indices = [Index("ownerId"), Index("visibility"), Index("entryEpochDay")]
)
data class DiaryEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val cloudId: String? = null,
    val ownerId: String,
    /**
     * 所属双人空间。v2 新增（B0.1 T05）。
     *
     * 为什么可空：
     * 1. 私密日记本来就不属于任何空间，写 null 是**正确语义**，不是"还没填"；
     * 2. 共享日记在离线创建、且本机还没拿到 coupleId 时也只能是 null。
     * 迁移时**不给历史数据编造空间 ID** —— 编了就等于把别人的数据挂到自己空间下。
     * 代价是：coupleId 为 null 的 SHARED 日记不允许进上传队列（入队前校验），
     * 等拿到空间后再补。这条限制写在 DiaryRepositoryImpl 里。
     */
    val coupleId: String? = null,
    val visibility: String,
    val title: String? = null,
    val content: String,
    val entryEpochDay: Long,
    val mood: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncStatus: String = "LOCAL_ONLY",
    val clientMutationId: String? = null,
    val serverVersion: Int = 0
)

/**
 * 每日双人问答（本地缓存）。
 *
 * v2 新增四个字段的原因（B0.1 T05）：
 * - coupleId：原表完全没有空间归属，两个空间同一天会撞在 questionEpochDay 的唯一索引上；
 * - updatedAt / syncStatus / serverVersion：原表跟其它表不一致，没有同步状态就没法参与增量同步。
 *
 * 关于 partnerAnswer 的安全约束（很重要）：
 * 这一列**只能在 revealed = true 时非空**。未揭晓前，服务端根本不下发对方答案，
 * 本地即使因为异常拿到了、也必须在读取时被 DAO 滤掉（见 DailyQuestionDao.observeAll 的 CASE WHEN）。
 * 只靠 UI 上不显示是不够的 —— 那叫"藏起来"，不叫"没有"。
 */
@Entity(
    tableName = "daily_question",
    // 复合唯一：同一空间同一天只能一题；不同空间同一天互不影响。
    // coupleId 为 NULL 时 SQLite 的 UNIQUE 允许重复，正好符合"离线未认领"的语义。
    indices = [Index(value = ["coupleId", "questionEpochDay"], unique = true)]
)
data class DailyQuestionEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val cloudId: String? = null,
    val coupleId: String? = null,
    val questionEpochDay: Long,
    val content: String,
    val myAnswer: String? = null,
    val partnerAnswer: String? = null,
    val revealed: Boolean = false,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0,
    @ColumnInfo(defaultValue = "LOCAL_ONLY") val syncStatus: String = "LOCAL_ONLY",
    @ColumnInfo(defaultValue = "0") val serverVersion: Int = 0
)

/**
 * 未来的信（本地只存"壳"与草稿）。
 *
 * v2 新增三个字段（B0.1 T05）：
 *
 * 1. [localDraftContent] —— 本地草稿正文。
 *    必要性：写信是一个"写一半会退出去"的动作，草稿必须能留在本机，
 *    否则用户打了五百字切一下后台就没了。这一列**永不同步**，
 *    也不允许出现在任何 Outbox 的 payload 里（入队函数会剥掉它）。
 *
 * 2. [status] —— 草稿与正式信件的区分。取值 DRAFT / LOCKED / UNLOCKED / CANCELLED。
 *    没有这个字段，一封还没发出去的草稿会被当成"未解锁的信"，界面上就会显示错。
 *
 * 3. [coupleId] —— 空间归属，可空（同 diary 的理由）。
 *
 * 仍然没有"正式正文"字段，这是故意的：
 * 正式信件正文以密文形式存在云端，解锁前服务端不下发；
 * 客户端拿到后才短暂缓存，且缓存不落这张表（B6 设计）。
 * 换句话说：localDraftContent 有值 = 这封还是草稿；没值 = 这是已发出的信。
 */
@Entity(tableName = "letter", indices = [Index("unlockAt")])
data class LetterEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val cloudId: String? = null,
    val coupleId: String? = null,
    val title: String,
    val unlockAt: Long,
    val senderId: String,
    val receiverId: String,
    /** 本地草稿正文；仅本机，永不同步。null = 已发出或还没开始写 */
    val localDraftContent: String? = null,
    /** DRAFT / LOCKED / UNLOCKED / CANCELLED */
    @ColumnInfo(defaultValue = "DRAFT") val status: String = "DRAFT",
    val hasLocalDraft: Boolean = false,
    val createdAt: Long
)

/**
 * 悄悄话（v2 新增表）。
 *
 * B0 阶段完全没有这张表，是 T05 指出的缺口之一：
 * 「悄悄话」是 B6 的核心功能，但一条消息都没有地方存。
 *
 * 字段设计说明：
 * - [cloudId] 与 [localId] 并存：本地自增主键负责离线创建，云端 UUID 负责跨设备识别；
 * - [coupleId] 可空：离线且未拿到空间时先存 null，入队前必须补齐（同 diary 规则）；
 * - [receiverId] 可空：null 表示"发给空间里的另一个人"，
 *   不写死具体 id 是因为离线时本机可能还不知道对方的 userId；
 * - [readAt] 已读时间，null = 未读。自己发出的消息不看 readAt（那是对方的已读）；
 * - [deletedAt] 软删除：消息删了要能同步"删除事件"，物理删会导致对方设备永远留一条；
 * - [syncStatus] 与其它表一致六态。
 *
 * 明确不做：这一版不写发送/接收业务，只建表与 DAO。
 */
@Entity(
    tableName = "message",
    indices = [
        Index("coupleId"),
        Index("createdAt"),
        Index("readAt"),
        Index("syncStatus")
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val cloudId: String? = null,
    val coupleId: String? = null,
    val senderId: String,
    /** null = 空间内另一个人 */
    val receiverId: String? = null,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val readAt: Long? = null,
    val deletedAt: Long? = null,
    val syncStatus: String = "LOCAL_ONLY",
    val clientMutationId: String? = null,
    val serverVersion: Int = 0
)

/** 愿望清单 */
@Entity(tableName = "wish", indices = [Index("doneAt"), Index("syncStatus")])
data class WishEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val cloudId: String? = null,
    val title: String,
    val note: String? = null,
    val targetAt: Long? = null,
    val doneAt: Long? = null,
    val priority: Int = 0,
    val createdAt: Long,
    /** 本地最后修改时间：勾选完成、改文案都要更新它，否则同步无法判断"谁更新" */
    val updatedAt: Long = createdAt,
    val deletedAt: Long? = null,
    val syncStatus: String = "LOCAL_ONLY",
    val clientMutationId: String? = null,
    val serverVersion: Int = 0
)

/** 下一次见面 */
@Entity(tableName = "meeting_plan", indices = [Index("plannedAt")])
data class MeetingPlanEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val cloudId: String? = null,
    val plannedAt: Long,
    val place: String? = null,
    val note: String? = null,
    val doneAt: Long? = null,
    val createdAt: Long,
    val deletedAt: Long? = null,
    val syncStatus: String = "LOCAL_ONLY",
    val clientMutationId: String? = null,
    val serverVersion: Int = 0
)

/**
 * 待同步队列（Outbox）。
 *
 * 关键设计：
 * - 主键是 taskId（UUID），不是自增，因为要"幂等"——同一个 taskId 重复入队会被忽略；
 * - clientMutationId 交给云端做唯一约束，重复上传不会产生第二条数据；
 * - 私密日记永远不会出现在这张表里（Repository 层拦死）。
 */
@Entity(
    tableName = "sync_task",
    indices = [Index("syncStatus"), Index("createdAt"), Index(value = ["clientMutationId"], unique = true)]
)
data class SyncTaskEntity(
    @PrimaryKey val taskId: String,
    val entityType: String,
    val entityId: Long,
    val operation: String,
    /** 提交给云端的字段快照（JSON 字符串）；私密内容禁止写进来 */
    val payload: String,
    val clientMutationId: String,
    val createdAt: Long,
    val retryCount: Int = 0,
    val syncStatus: String = "PENDING_UPLOAD",
    val lastError: String? = null
)

/** 同步游标：每个数据域一行（memory / diary / wish / meeting ...） */
@Entity(tableName = "sync_cursor")
data class SyncCursorEntity(
    @PrimaryKey val dataDomain: String,
    val lastSyncedAt: Long = 0L,
    val updatedAt: Long = 0L
)
