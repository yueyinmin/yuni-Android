package com.mulin.us.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.mulin.us.data.local.entity.CoupleEntity
import com.mulin.us.data.local.entity.DailyQuestionEntity
import com.mulin.us.data.local.entity.DiaryEntity
import com.mulin.us.data.local.entity.LetterEntity
import com.mulin.us.data.local.entity.LocalUserEntity
import com.mulin.us.data.local.entity.MeetingPlanEntity
import com.mulin.us.data.local.entity.MemoryEntity
import com.mulin.us.data.local.entity.MemoryPhotoEntity
import com.mulin.us.data.local.entity.MessageEntity
import com.mulin.us.data.local.entity.SyncCursorEntity
import com.mulin.us.data.local.entity.SyncTaskEntity
import com.mulin.us.data.local.entity.WishEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO 层：只写 SQL，不写业务判断。
 * 所有"要不要上传""是不是私密"的判断都在 Repository，不在这里。
 */

@Dao
interface UserDao {
    @Query("SELECT * FROM local_user ORDER BY createdAt DESC LIMIT 1")
    fun observeCurrent(): Flow<LocalUserEntity?>

    @Query("SELECT * FROM local_user ORDER BY createdAt DESC LIMIT 1")
    suspend fun current(): LocalUserEntity?

    @Upsert
    suspend fun upsert(entity: LocalUserEntity)

    /** 退出登录不会调它。只有"清空本机资料"这种用户显式操作用得到。 */
    @Query("DELETE FROM local_user")
    suspend fun clear()
}

@Dao
interface CoupleDao {
    @Query("SELECT * FROM couple_profile WHERE status = 'active' ORDER BY createdAt DESC LIMIT 1")
    fun observeActive(): Flow<CoupleEntity?>

    @Query("SELECT * FROM couple_profile WHERE status = 'active' ORDER BY createdAt DESC LIMIT 1")
    suspend fun active(): CoupleEntity?

    @Upsert
    suspend fun upsert(entity: CoupleEntity)

    @Query("UPDATE couple_profile SET status = 'dissolved' WHERE id = :id")
    suspend fun markDissolved(id: String)

    @Query("UPDATE couple_profile SET relationshipStartEpochDay = :epochDay WHERE id = :id")
    suspend fun setRelationshipStart(id: String, epochDay: Long)

    @Query("UPDATE couple_profile SET startDateIso = :iso, relationshipStartEpochDay = :epochDay, startDateUpdatedAt = :updatedAt WHERE id = :id")
    suspend fun setStartDateIso(id: String, iso: String, epochDay: Long, updatedAt: Long)
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory WHERE deletedAt IS NULL ORDER BY COALESCE(happenedAt, createdAt) DESC")
    fun observeActive(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memory WHERE localId = :localId")
    suspend fun byId(localId: Long): MemoryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: MemoryEntity): Long

    @Update
    suspend fun update(entity: MemoryEntity)

    @Query("UPDATE memory SET deletedAt = :now, updatedAt = :now, syncStatus = 'PENDING_DELETE' WHERE localId = :localId")
    suspend fun softDelete(localId: Long, now: Long)

    @Query("SELECT COUNT(*) FROM memory WHERE deletedAt IS NULL")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM memory WHERE syncStatus = 'PENDING_UPLOAD' ORDER BY updatedAt ASC LIMIT :limit")
    suspend fun pendingUpload(limit: Int): List<MemoryEntity>

    @Query("UPDATE memory SET syncStatus = 'SYNCED', cloudId = COALESCE(:cloudId, cloudId), serverVersion = :version WHERE localId = :localId")
    suspend fun markSynced(localId: Long, cloudId: String?, version: Int)

    @Query("UPDATE memory SET syncStatus = 'FAILED' WHERE localId = :localId")
    suspend fun markFailed(localId: Long)

    @Query("UPDATE memory SET syncStatus = 'CONFLICT' WHERE localId = :localId")
    suspend fun markConflict(localId: Long)

    @Query("SELECT * FROM memory WHERE clientMutationId = :mutationId LIMIT 1")
    suspend fun byMutationId(mutationId: String): MemoryEntity?
}

@Dao
interface MemoryPhotoDao {
    @Query("SELECT * FROM memory_photo WHERE memoryLocalId = :memoryLocalId ORDER BY id ASC")
    suspend fun byMemory(memoryLocalId: Long): List<MemoryPhotoEntity>

    /** 4.8 持久化：UI 订阅这个 Flow，重启 App 后缩略图仍在 */
    @Query("SELECT * FROM memory_photo WHERE memoryLocalId = :memoryLocalId ORDER BY id ASC")
    fun observeByMemory(memoryLocalId: Long): Flow<List<MemoryPhotoEntity>>

    /** 时间线聚合用：一次读全部照片，避免 N+1 查询 */
    @Query("SELECT * FROM memory_photo ORDER BY memoryLocalId ASC, id ASC")
    fun observeAll(): Flow<List<MemoryPhotoEntity>>

    @Insert
    suspend fun insert(entity: MemoryPhotoEntity): Long

    @Query("DELETE FROM memory_photo WHERE memoryLocalId = :memoryLocalId")
    suspend fun deleteByMemory(memoryLocalId: Long)

    @Query("SELECT COUNT(*) FROM memory_photo WHERE cloudFileId IS NULL")
    fun observeNotUploadedCount(): Flow<Int>
}

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary WHERE deletedAt IS NULL ORDER BY entryEpochDay DESC, createdAt DESC")
    fun observeActive(): Flow<List<DiaryEntity>>

    @Query("SELECT * FROM diary WHERE deletedAt IS NULL AND visibility = :visibility ORDER BY entryEpochDay DESC, createdAt DESC")
    fun observeByVisibility(visibility: String): Flow<List<DiaryEntity>>

    @Query("SELECT * FROM diary WHERE deletedAt IS NULL AND (visibility = 'SHARED' OR ownerId = :ownerId) ORDER BY entryEpochDay DESC")
    fun observeVisibleTo(ownerId: String): Flow<List<DiaryEntity>>

    @Query("SELECT * FROM diary WHERE localId = :localId")
    suspend fun byId(localId: Long): DiaryEntity?

    @Insert
    suspend fun insert(entity: DiaryEntity): Long

    @Update
    suspend fun update(entity: DiaryEntity)

    @Query("UPDATE diary SET deletedAt = :now, updatedAt = :now, syncStatus = 'PENDING_DELETE' WHERE localId = :localId")
    suspend fun softDelete(localId: Long, now: Long)

    @Query("SELECT COUNT(*) FROM diary WHERE deletedAt IS NULL")
    fun observeCount(): Flow<Int>

    /**
     * 只有 SHARED 且 coupleId 非空才可能被上传。
     * 两层条件都在 SQL 里兜住，防止有人绕过 Repository 写脏数据：
     * 没认领空间的共享日记传上去，云端 RLS 也只会拒绝，白耗一次请求。
     */
    @Query("SELECT * FROM diary WHERE syncStatus = 'PENDING_UPLOAD' AND visibility = 'SHARED' AND coupleId IS NOT NULL ORDER BY updatedAt ASC LIMIT :limit")
    suspend fun pendingUploadShared(limit: Int): List<DiaryEntity>

    @Query("UPDATE diary SET syncStatus = 'SYNCED', cloudId = COALESCE(:cloudId, cloudId), serverVersion = :version WHERE localId = :localId")
    suspend fun markSynced(localId: Long, cloudId: String?, version: Int)

    @Query("UPDATE diary SET syncStatus = 'FAILED' WHERE localId = :localId")
    suspend fun markFailed(localId: Long)

    @Query("UPDATE diary SET syncStatus = 'CONFLICT' WHERE localId = :localId")
    suspend fun markConflict(localId: Long)

    @Query("SELECT * FROM diary WHERE clientMutationId = :mutationId LIMIT 1")
    suspend fun byMutationId(mutationId: String): DiaryEntity?

    @Query("SELECT * FROM diary WHERE cloudId = :cloudId LIMIT 1")
    suspend fun byCloudId(cloudId: String): DiaryEntity?
}

@Dao
interface DailyQuestionDao {
    /**
     * 读当日问答列表。
     *
     * 注意 partnerAnswer 那一列的 CASE WHEN —— 这是**读取层的兜底**：
     * 未揭晓（revealed = 0）时，无论数据库里对方答案是不是有值，都返回 NULL。
     *
     * 为什么要在 SQL 里做，而不是在 UI 里 `if (revealed) 显示 else 隐藏`：
     * UI 隐藏只是"看不见"，数据仍在内存里，任何一处忘记判断就会漏。
     * 数据根本没读出来，才是真正的"没有"。
     *
     * 真正的防线在服务端（RLS + 云函数），这里只是本地最后一道。
     */
    @Query(
        """
        SELECT localId, cloudId, coupleId, questionEpochDay, content,
               myAnswer,
               CASE WHEN revealed = 1 THEN partnerAnswer ELSE NULL END AS partnerAnswer,
               revealed, createdAt, updatedAt, syncStatus, serverVersion
        FROM daily_question
        ORDER BY questionEpochDay DESC
        """
    )
    fun observeAll(): Flow<List<DailyQuestionEntity>>

    /**
     * 时间线专用：只返回已揭晓的问答。
     * 这里在 SQL 层再挡一次，避免未揭晓数据因为任何脏写泄漏到时间线聚合。
     */
    @Query(
        """
        SELECT localId, cloudId, coupleId, questionEpochDay, content,
               myAnswer, partnerAnswer, revealed, createdAt, updatedAt, syncStatus, serverVersion
        FROM daily_question
        WHERE revealed = 1
        ORDER BY questionEpochDay DESC
        """
    )
    fun observeRevealed(): Flow<List<DailyQuestionEntity>>

    /** 取某一天那一题（epochDay）。用于"今天答了没"的判断，B5 使用。 */
    @Query(
        """
        SELECT localId, cloudId, coupleId, questionEpochDay, content,
               myAnswer,
               CASE WHEN revealed = 1 THEN partnerAnswer ELSE NULL END AS partnerAnswer,
               revealed, createdAt, updatedAt, syncStatus, serverVersion
        FROM daily_question
        WHERE questionEpochDay = :epochDay
        LIMIT 1
        """
    )
    suspend fun byDay(epochDay: Long): DailyQuestionEntity?

    @Upsert
    suspend fun upsert(entity: DailyQuestionEntity)

    @Query("SELECT COUNT(*) FROM daily_question")
    fun observeCount(): Flow<Int>
}

/**
 * 悄悄话 DAO（v2 新增，B0.1 T05）。
 *
 * 只提供"读写消息"所需的最小方法集合，不写业务判断：
 * 能不能发、发给谁、要不要签名，都是 B6 的 Repository 该管的事。
 */
@Dao
interface MessageDao {
    /** 某空间最近的消息；不含已删除 */
    @Query(
        """
        SELECT * FROM message
        WHERE deletedAt IS NULL AND coupleId = :coupleId
        ORDER BY createdAt DESC
        LIMIT :limit
        """
    )
    fun observeRecent(coupleId: String, limit: Int = 100): Flow<List<MessageEntity>>

    /**
     * 未读数：别人发给我的、还没读的。
     *
     * 两个条件缺一不可：
     * - 排除自己发的（否则自己发的消息会被算成未读）；
     * - receiverId 允许为 null（发给"空间里另一个人"的写法），所以用 OR 兜住。
     */
    @Query(
        """
        SELECT COUNT(*) FROM message
        WHERE deletedAt IS NULL
          AND coupleId = :coupleId
          AND readAt IS NULL
          AND senderId != :myId
          AND (receiverId = :myId OR receiverId IS NULL)
        """
    )
    fun observeUnreadCount(coupleId: String, myId: String): Flow<Int>

    @Query("SELECT * FROM message WHERE localId = :localId")
    suspend fun byId(localId: Long): MessageEntity?

    @Insert
    suspend fun insert(entity: MessageEntity): Long

    @Update
    suspend fun update(entity: MessageEntity)

    /** 已读只标一次：已经读过的不再覆盖时间 */
    @Query("UPDATE message SET readAt = :now WHERE localId = :localId AND readAt IS NULL")
    suspend fun markRead(localId: Long, now: Long)

    @Query("UPDATE message SET deletedAt = :now, updatedAt = :now, syncStatus = 'PENDING_DELETE' WHERE localId = :localId")
    suspend fun softDelete(localId: Long, now: Long)

    @Query("SELECT * FROM message WHERE syncStatus = 'PENDING_UPLOAD' AND coupleId IS NOT NULL ORDER BY createdAt ASC LIMIT :limit")
    suspend fun pendingUpload(limit: Int): List<MessageEntity>

    @Query("UPDATE message SET syncStatus = 'SYNCED', cloudId = COALESCE(:cloudId, cloudId), serverVersion = :version WHERE localId = :localId")
    suspend fun markSynced(localId: Long, cloudId: String?, version: Int)

    @Query("UPDATE message SET syncStatus = 'FAILED' WHERE localId = :localId")
    suspend fun markFailed(localId: Long)

    @Query("SELECT COUNT(*) FROM message WHERE deletedAt IS NULL")
    fun observeCount(): Flow<Int>
}

@Dao
interface LetterDao {
    @Query("SELECT * FROM letter ORDER BY unlockAt ASC")
    fun observeAll(): Flow<List<LetterEntity>>

    /** 时间线专用：只返回服务端确认解锁的信，未解锁/草稿/CANCELLED 一律排除 */
    @Query("SELECT * FROM letter WHERE status = 'UNLOCKED' ORDER BY unlockAt DESC")
    fun observeUnlocked(): Flow<List<LetterEntity>>

    @Query("SELECT * FROM letter WHERE cloudId = :cloudId LIMIT 1")
    suspend fun byCloudId(cloudId: String): LetterEntity?

    @Upsert
    suspend fun upsert(entity: LetterEntity)

    @Query("SELECT COUNT(*) FROM letter")
    fun observeCount(): Flow<Int>
}

@Dao
interface WishDao {
    @Query("SELECT * FROM wish WHERE deletedAt IS NULL ORDER BY doneAt IS NOT NULL, priority DESC, createdAt DESC")
    fun observeActive(): Flow<List<WishEntity>>

    @Insert
    suspend fun insert(entity: WishEntity): Long

    @Update
    suspend fun update(entity: WishEntity)

    @Query("UPDATE wish SET doneAt = :doneAt, updatedAt = :now, syncStatus = 'PENDING_UPLOAD' WHERE localId = :localId")
    suspend fun setDone(localId: Long, doneAt: Long?, now: Long)

    @Query("UPDATE wish SET deletedAt = :now, syncStatus = 'PENDING_DELETE' WHERE localId = :localId")
    suspend fun softDelete(localId: Long, now: Long)

    @Query("SELECT COUNT(*) FROM wish WHERE deletedAt IS NULL")
    fun observeCount(): Flow<Int>

    @Query("UPDATE wish SET syncStatus = 'SYNCED', cloudId = COALESCE(:cloudId, cloudId) WHERE localId = :localId")
    suspend fun markSynced(localId: Long, cloudId: String?)

    @Query("UPDATE wish SET syncStatus = 'FAILED' WHERE localId = :localId")
    suspend fun markFailed(localId: Long)

    @Query("SELECT * FROM wish WHERE cloudId = :cloudId LIMIT 1")
    suspend fun byCloudId(cloudId: String): WishEntity?
}

@Dao
interface MeetingPlanDao {
    @Query("SELECT * FROM meeting_plan WHERE deletedAt IS NULL ORDER BY plannedAt ASC")
    fun observeActive(): Flow<List<MeetingPlanEntity>>

    @Insert
    suspend fun insert(entity: MeetingPlanEntity): Long

    @Query("UPDATE meeting_plan SET deletedAt = :now, syncStatus = 'PENDING_DELETE' WHERE localId = :localId")
    suspend fun softDelete(localId: Long, now: Long)

    @Query("SELECT COUNT(*) FROM meeting_plan WHERE deletedAt IS NULL")
    fun observeCount(): Flow<Int>

    @Query("UPDATE meeting_plan SET syncStatus = 'SYNCED', cloudId = COALESCE(:cloudId, cloudId) WHERE localId = :localId")
    suspend fun markSynced(localId: Long, cloudId: String?)

    @Query("UPDATE meeting_plan SET syncStatus = 'FAILED' WHERE localId = :localId")
    suspend fun markFailed(localId: Long)

    @Query("SELECT * FROM meeting_plan WHERE cloudId = :cloudId LIMIT 1")
    suspend fun byCloudId(cloudId: String): MeetingPlanEntity?
}

@Dao
interface SyncTaskDao {
    /**
     * ABORT 冲突策略 + clientMutationId 唯一索引 = 天然去重。
     * 同一次改动被重复入队时，第二次会抛异常，Repository 捕获后忽略即可。
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: SyncTaskEntity)

    /** 同一条记录重复入队时用它覆盖旧的（只保留最后一次改动） */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: SyncTaskEntity)

    @Query("SELECT * FROM sync_task WHERE syncStatus = 'PENDING_UPLOAD' ORDER BY createdAt ASC LIMIT :limit")
    suspend fun pending(limit: Int): List<SyncTaskEntity>

    @Query("SELECT COUNT(*) FROM sync_task WHERE syncStatus = 'PENDING_UPLOAD'")
    fun observePendingCount(): Flow<Int>

    /**
     * 取同一实体还没发出去的那一条（用于覆盖式入队）。
     * 返回整行而不是只返回数量：覆盖时必须沿用旧 taskId，
     * 否则每次都是新 UUID，REPLACE 永远不会命中旧行，同一条改动会发两次。
     */
    @Query("SELECT * FROM sync_task WHERE entityType = :type AND entityId = :entityId AND syncStatus = 'PENDING_UPLOAD' ORDER BY createdAt ASC LIMIT 1")
    suspend fun pendingTaskForEntity(type: String, entityId: Long): SyncTaskEntity?

    @Query("DELETE FROM sync_task WHERE taskId = :taskId")
    suspend fun delete(taskId: String)

    @Query("UPDATE sync_task SET retryCount = retryCount + 1, lastError = :error WHERE taskId = :taskId")
    suspend fun bumpRetry(taskId: String, error: String?)

    @Query("UPDATE sync_task SET syncStatus = :status WHERE taskId = :taskId")
    suspend fun setStatus(taskId: String, status: String)

    @Query("SELECT COUNT(*) FROM sync_task")
    suspend fun total(): Int
}

@Dao
interface SyncCursorDao {
    @Query("SELECT * FROM sync_cursor WHERE dataDomain = :domain")
    suspend fun get(domain: String): SyncCursorEntity?

    @Query("SELECT * FROM sync_cursor")
    fun observeAll(): Flow<List<SyncCursorEntity>>

    @Upsert
    suspend fun upsert(entity: SyncCursorEntity)
}
