package com.mulin.us.data.sync

import com.mulin.us.core.common.SafeLog
import com.mulin.us.data.local.dao.SyncCursorDao
import com.mulin.us.data.local.dao.SyncTaskDao
import com.mulin.us.data.local.entity.SyncTaskEntity
import com.mulin.us.data.local.entity.MemoryEntity
import com.mulin.us.domain.model.SyncOperation
import com.mulin.us.domain.model.SyncStatus
import org.json.JSONObject
import java.util.UUID

/**
 * Outbox 写入器。
 *
 * 职责只有一件事：把"这次改动"变成一个可重放的任务。
 * 它不判断能不能上传——那是各 Repository 的事（私密日记压根不会调它）。
 *
 * 两条防止重复的机制：
 * 1. 入队前先查同一实体有没有未上传任务：有就 REPLACE 掉旧的，只发最后一次快照；
 * 2. clientMutationId 有唯一索引，云端也会去重。
 */
class OutboxWriter(
    private val taskDao: SyncTaskDao,
    private val cursorDao: SyncCursorDao
) {

    suspend fun enqueueMemory(entity: MemoryEntity, operation: SyncOperation, mutationId: String) {
        val payload = JSONObject()
            .put("client_mutation_id", mutationId)
            .put("title", entity.title)
            .put("content", entity.content)
            .put("happened_at", entity.happenedAt ?: JSONObject.NULL)
            .put("mood", entity.mood ?: JSONObject.NULL)
            .put("deleted_at", entity.deletedAt ?: JSONObject.NULL)
            .toString()
        enqueue("memory", entity.localId, operation, payload, mutationId)
    }

    /**
     * 日记入队。
     * 注意：调用方必须已经确认 visibility == SHARED。
     * 这里再兜一次，防止以后有人手滑把私密日记传上来。
     */
    suspend fun enqueueDiary(
        localId: Long,
        visibility: String,
        payload: JSONObject,
        operation: SyncOperation,
        mutationId: String
    ) {
        if (visibility != "SHARED") {
            SafeLog.w("Outbox", "refuse to enqueue PRIVATE diary localId=$localId")
            return
        }
        enqueue("diary", localId, operation, payload.toString(), mutationId)
    }

    suspend fun enqueue(
        entityType: String,
        entityId: Long,
        operation: SyncOperation,
        payloadJson: String,
        mutationId: String? = null
    ) {
        val mutation = mutationId ?: UUID.randomUUID().toString()
        val pending = taskDao.pendingForEntity(entityType, entityId)
        val task = SyncTaskEntity(
            taskId = UUID.randomUUID().toString(),
            entityType = entityType,
            entityId = entityId,
            operation = operation.name,
            payload = payloadJson,
            clientMutationId = mutation,
            createdAt = System.currentTimeMillis(),
            retryCount = 0,
            syncStatus = SyncStatus.PENDING_UPLOAD.name
        )
        if (pending > 0) {
            // 同一条记录还挂着没发出去的任务：用新的覆盖旧的（只发最终状态）
            taskDao.insertOrReplace(task)
            SafeLog.d("Outbox", "replaced pending task for $entityType#$entityId")
        } else {
            taskDao.insert(task)
            SafeLog.d("Outbox", "enqueued ${operation.name} $entityType#$entityId")
        }
    }

    suspend fun markSynced(taskId: String) {
        taskDao.delete(taskId)
    }

    suspend fun markFailed(taskId: String, error: String?) {
        taskDao.bumpRetry(taskId, error)
    }

    suspend fun advanceCursor(domain: String, serverUpdatedAt: Long) {
        cursorDao.upsert(
            com.mulin.us.data.local.entity.SyncCursorEntity(
                dataDomain = domain,
                lastSyncedAt = serverUpdatedAt,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
