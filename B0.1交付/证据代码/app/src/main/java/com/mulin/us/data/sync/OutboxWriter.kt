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
     * 日记入队。两道闸门，缺一不可：
     * 1. visibility 必须是 SHARED —— 私密日记永不入队；
     * 2. coupleId 必须非空 —— 没认领空间的共享日记传上去只会被 RLS 拒绝，
     *    而且迁移过来的历史数据本来就没有空间归属，不能替它猜一个。
     *
     * 这两条在这里兜底，Repository 里也会判一次；故意写两遍，
     * 因为"私密日记上云"是一次就 irreversible 的事故。
     */
    suspend fun enqueueDiary(
        localId: Long,
        visibility: String,
        coupleId: String?,
        payload: JSONObject,
        operation: SyncOperation,
        mutationId: String
    ) {
        if (visibility != "SHARED") {
            SafeLog.w("Outbox", "refuse to enqueue PRIVATE diary localId=$localId")
            return
        }
        if (coupleId.isNullOrBlank()) {
            SafeLog.w("Outbox", "refuse to enqueue diary without coupleId localId=$localId")
            return
        }
        // 空间归属写进 payload，云端也按这一列做 RLS 校验
        payload.put("couple_id", coupleId)
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
        // 复用旧 taskId 才能真正覆盖旧任务；换新 UUID 的话 REPLACE 不命中，
        // 同一条改动会留下两条待上传，云端去重前会多打一次请求。
        val pending = taskDao.pendingTaskForEntity(entityType, entityId)
        val task = SyncTaskEntity(
            taskId = pending?.taskId ?: UUID.randomUUID().toString(),
            entityType = entityType,
            entityId = entityId,
            operation = operation.name,
            payload = payloadJson,
            clientMutationId = mutation,
            createdAt = System.currentTimeMillis(),
            retryCount = 0,
            syncStatus = SyncStatus.PENDING_UPLOAD.name
        )
        if (pending != null) {
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
