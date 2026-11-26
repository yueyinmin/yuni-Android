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

    /** 只有 SHARED 才可能被上传；这里再兜一层，防止有人绕过 Repository 写脏数据 */
    @Query("SELECT * FROM diary WHERE syncStatus = 'PENDING_UPLOAD' AND visibility = 'SHARED' ORDER BY updatedAt ASC LIMIT :limit")
    suspend fun pendingUploadShared(limit: Int): List<DiaryEntity>

    @Query("UPDATE diary SET syncStatus = 'SYNCED', cloudId = COALESCE(:cloudId, cloudId), serverVersion = :version WHERE localId = :localId")
    suspend fun markSynced(localId: Long, cloudId: String?, version: Int)

    @Query("UPDATE diary SET syncStatus = 'FAILED' WHERE localId = :localId")
    suspend fun markFailed(localId: Long)

    @Query("UPDATE diary SET syncStatus = 'CONFLICT' WHERE localId = :localId")
    suspend fun markConflict(localId: Long)
}

@Dao
interface DailyQuestionDao {
    @Query("SELECT * FROM daily_question ORDER BY questionEpochDay DESC")
    fun observeAll(): Flow<List<DailyQuestionEntity>>

    @Upsert
    suspend fun upsert(entity: DailyQuestionEntity)

    @Query("SELECT COUNT(*) FROM daily_question")
    fun observeCount(): Flow<Int>
}

@Dao
interface LetterDao {
    @Query("SELECT * FROM letter ORDER BY unlockAt ASC")
    fun observeAll(): Flow<List<LetterEntity>>

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

    @Query("SELECT COUNT(*) FROM sync_task WHERE entityType = :type AND entityId = :entityId AND syncStatus = 'PENDING_UPLOAD'")
    suspend fun pendingForEntity(type: String, entityId: Long): Int

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
