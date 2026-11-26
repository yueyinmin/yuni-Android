package com.mulin.us.data.local.entity

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

/** 每日双人问答 */
@Entity(tableName = "daily_question", indices = [Index(value = ["questionEpochDay"], unique = true)])
data class DailyQuestionEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val cloudId: String? = null,
    val questionEpochDay: Long,
    val content: String,
    val myAnswer: String? = null,
    val partnerAnswer: String? = null,
    val revealed: Boolean = false,
    val createdAt: Long
)

/** 未来的信（B0 只存壳，不存正文） */
@Entity(tableName = "letter", indices = [Index("unlockAt")])
data class LetterEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val cloudId: String? = null,
    val title: String,
    val unlockAt: Long,
    val senderId: String,
    val receiverId: String,
    val hasLocalDraft: Boolean = false,
    val createdAt: Long
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
