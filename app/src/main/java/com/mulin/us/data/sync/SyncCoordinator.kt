package com.mulin.us.data.sync

import com.mulin.us.core.common.AppConfig
import com.mulin.us.core.common.SafeLog
import com.mulin.us.data.local.YuniDatabase
import com.mulin.us.data.local.entity.MemoryEntity
import com.mulin.us.data.local.entity.SyncTaskEntity
import com.mulin.us.data.remote.PushItem
import com.mulin.us.data.remote.RemoteResult
import com.mulin.us.data.remote.SyncRemoteDataSource
import com.mulin.us.domain.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 同步协调器（全 App 只有一个实例）。
 *
 * 为什么要单实例 + Mutex：
 * WorkManager 重试、前台轮询、用户手动刷新这三条路会同时想同步。
 * 不加锁就一定会出现"同一条数据被两个协程同时推送"，进而重复上传。
 * 加锁是最便宜的解法，也比"用时间戳判断"可靠。
 */
class SyncCoordinator(
    private val db: YuniDatabase,
    private val syncRemote: SyncRemoteDataSource,
    private val outbox: OutboxWriter
) {

    private val mutex = Mutex()

    /** 每个数据域一次最多拉多少条 */
    private val pullPageSize = 200

    /** 每次最多推多少条 */
    private val pushBatchSize = 20

    /** 增量同步覆盖的域。B0 只有 memory，现补齐 diary/wish/meeting_plan */
    private val PULL_DOMAINS = listOf("memory", "diary", "wish", "meeting_plan")

    val isCloudAvailable: Boolean get() = AppConfig.networkEnabled

    suspend fun runOnce(reason: String): SyncReport = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!AppConfig.networkEnabled) {
                SyncStateStore.lastResult.value = "本地模式：未连接云端（0 请求）"
                return@withContext SyncReport(skipped = true, message = "local-mode")
            }

            val startedAt = System.currentTimeMillis()
            var pushed = 0
            var conflicted = 0
            var failed = 0

            // ---------- 1. 推送 ----------
            val tasks = db.syncTaskDao().pending(pushBatchSize)
            if (tasks.isNotEmpty()) {
                val items = tasks.map {
                    PushItem(
                        clientMutationId = it.clientMutationId,
                        entityType = it.entityType,
                        operation = it.operation,
                        payloadJson = it.payload
                    )
                }
                when (val r = syncRemote.pushChanges(items)) {
                    is RemoteResult.Success -> {
                        val outcome = r.data
                        outcome.accepted.forEach { mutation ->
                            tasks.firstOrNull { it.clientMutationId == mutation }?.let { task ->
                                applySynced(task)
                                db.syncTaskDao().delete(task.taskId)
                                pushed++
                            }
                        }
                        // unchanged = 云端已有（幂等命中）：同样算成功
                        outcome.unchanged.forEach { mutation ->
                            tasks.firstOrNull { it.clientMutationId == mutation }?.let { task ->
                                applySynced(task)
                                db.syncTaskDao().delete(task.taskId)
                                pushed++
                            }
                        }
                        outcome.forbidden.forEach { mutation ->
                            tasks.firstOrNull { it.clientMutationId == mutation }?.let { task ->
                                db.syncTaskDao().setStatus(task.taskId, SyncStatus.FAILED.name)
                                failed++
                            }
                        }
                        outcome.conflicts.keys.forEach { mutation ->
                            tasks.firstOrNull { it.clientMutationId == mutation }?.let { task ->
                                markConflict(task)
                                conflicted++
                            }
                        }
                    }
                    is RemoteResult.Unauthorized, is RemoteResult.Forbidden -> {
                        // 认证/权限问题不做重试：重试一万次也还是没权限
                        tasks.forEach { db.syncTaskDao().setStatus(it.taskId, SyncStatus.FAILED.name) }
                        failed = tasks.size
                    }
                    else -> {
                        tasks.forEach { db.syncTaskDao().bumpRetry(it.taskId, describe(r)) }
                        failed = tasks.size
                    }
                }
            }

            // ---------- 2. 拉取（全四个域，路线图 2.5）----------
            val pulled = PULL_DOMAINS.sumOf { pullDomain(it) }

            val cost = System.currentTimeMillis() - startedAt
            val message = if (pushed == 0 && pulled == 0 && failed == 0 && conflicted == 0) {
                "已是最新（$reason）"
            } else {
                "推送 $pushed 条 / 拉取 $pulled 条 / 冲突 $conflicted / 失败 $failed（${cost}ms，$reason）"
            }
            SyncStateStore.lastSyncAt.value = System.currentTimeMillis()
            SyncStateStore.lastResult.value = message
            SafeLog.i("Sync", message)
            SyncReport(pushed = pushed, pulled = pulled, conflicts = conflicted, failed = failed)
        }
    }

    /** 拉一个数据域，返回条数；B0 只落地 memory，其余等 B4 */
    private suspend fun pullDomain(domain: String): Int {
        val cursor = db.syncCursorDao().get(domain)?.lastSyncedAt ?: 0L
        return when (val r = syncRemote.pullChanges(domain, if (cursor > 0) cursor else null, pullPageSize)) {
            is RemoteResult.Success -> {
                var maxTs = cursor
                r.data.forEach { item ->
                    if (item.serverUpdatedAt > maxTs) maxTs = item.serverUpdatedAt
                    if (item.deleted) return@forEach
                    upsertPulled(domain, item.payloadJson, item.cloudId, item.version)
                }
                if (maxTs > 0) outbox.advanceCursor(domain, maxTs)
                r.data.size
            }
            else -> {
                SafeLog.w("Sync", "pull $domain failed: ${describe(r)}")
                0
            }
        }
    }

    /** 按数据域分派落库：自己是本地新增就回填，对方新增就插入 */
    private suspend fun upsertPulled(domain: String, payload: String, cloudId: String, version: Int) {
        when (domain) {
            "memory" -> upsertPulledMemory(payload, cloudId, version)
            "diary" -> upsertPulledDiary(payload, cloudId, version)
            "wish" -> upsertPulledWish(payload, cloudId)
            "meeting_plan" -> upsertPulledMeeting(payload, cloudId)
        }
    }

    private suspend fun upsertPulledDiary(payload: String, cloudId: String, version: Int) {
        val obj = runCatching { JSONObject(payload) }.getOrNull() ?: return
        val mutationId = obj.optString("client_mutation_id")
        val existing = if (mutationId.isNotBlank()) db.diaryDao().byMutationId(mutationId) else null
        if (existing != null) {
            db.diaryDao().markSynced(existing.localId, cloudId, version)
            return
        }
        val ownerId = obj.optString("owner_id").ifBlank { return }
        val entryDay = if (obj.isNull("entry_epoch_day")) 0L else obj.optLong("entry_epoch_day")
        val existingByCloud = db.diaryDao().byCloudId(cloudId)
        val entity = com.mulin.us.data.local.entity.DiaryEntity(
            localId = existingByCloud?.localId ?: 0,
            cloudId = cloudId,
            coupleId = obj.optString("couple_id").ifBlank { null },
            ownerId = ownerId,
            visibility = obj.optString("visibility", "SHARED"),
            title = obj.optString("title").ifBlank { null },
            content = obj.optString("content"),
            entryEpochDay = entryDay,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = com.mulin.us.domain.model.SyncStatus.SYNCED.name,
            clientMutationId = mutationId.ifBlank { null },
            serverVersion = version
        )
        if (existingByCloud != null) db.diaryDao().update(entity) else db.diaryDao().insert(entity)
    }

    private suspend fun upsertPulledWish(payload: String, cloudId: String) {
        val obj = runCatching { JSONObject(payload) }.getOrNull() ?: return
        val mutationId = obj.optString("client_mutation_id")
        val existing = db.wishDao().byCloudId(cloudId)
        if (existing == null) {
            db.wishDao().insert(com.mulin.us.data.local.entity.WishEntity(
                cloudId = cloudId,
                title = obj.optString("title"),
                note = obj.optString("note").ifBlank { null },
                targetAt = if (obj.isNull("target_at")) null else obj.optLong("target_at"),
                doneAt = if (obj.isNull("done_at")) null else obj.optLong("done_at"),
                createdAt = System.currentTimeMillis(),
                syncStatus = com.mulin.us.domain.model.SyncStatus.SYNCED.name,
                clientMutationId = mutationId.ifBlank { null }
            ))
        }
    }

    private suspend fun upsertPulledMeeting(payload: String, cloudId: String) {
        val obj = runCatching { JSONObject(payload) }.getOrNull() ?: return
        if (db.meetingPlanDao().byCloudId(cloudId) != null) return
        db.meetingPlanDao().insert(com.mulin.us.data.local.entity.MeetingPlanEntity(
            cloudId = cloudId,
            plannedAt = if (obj.isNull("planned_at")) System.currentTimeMillis() else obj.optLong("planned_at"),
            place = obj.optString("place").ifBlank { null },
            note = obj.optString("note").ifBlank { null },
            doneAt = if (obj.isNull("done_at")) null else obj.optLong("done_at"),
            createdAt = System.currentTimeMillis(),
            syncStatus = com.mulin.us.domain.model.SyncStatus.SYNCED.name,
            clientMutationId = obj.optString("client_mutation_id").ifBlank { null }
        ))
    }

    /**
     * 落库时按 clientMutationId 找本地行：
     * 找到 → 说明是自己推上去又拉回来的（或本机已有），只回填 cloudId 与版本；
     * 找不到 → 对方设备新增的，直接插一条。
     */
    private suspend fun upsertPulledMemory(payload: String, cloudId: String, version: Int) {
        val obj = runCatching { JSONObject(payload) }.getOrNull() ?: return
        val mutationId = obj.optString("client_mutation_id")
        val existing = if (mutationId.isNotBlank()) db.memoryDao().byMutationId(mutationId) else null
        if (existing != null) {
            db.memoryDao().markSynced(existing.localId, cloudId, version)
        } else {
            db.memoryDao().insert(
                MemoryEntity(
                    cloudId = cloudId,
                    title = obj.optString("title"),
                    content = obj.optString("content"),
                    happenedAt = if (obj.isNull("happened_at")) null else obj.optLong("happened_at"),
                    mood = obj.optString("mood").ifBlank { null },
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.SYNCED.name,
                    clientMutationId = mutationId.ifBlank { null },
                    serverVersion = version
                )
            )
        }
    }

    private suspend fun applySynced(task: SyncTaskEntity) {
        when (task.entityType) {
            "memory" -> db.memoryDao().markSynced(task.entityId, null, 1)
            "diary" -> db.diaryDao().markSynced(task.entityId, null, 1)
            "wish" -> db.wishDao().markSynced(task.entityId, null)
            "meeting_plan", "meeting" -> db.meetingPlanDao().markSynced(task.entityId, null)
            else -> Unit
        }
    }

    private suspend fun markConflict(task: SyncTaskEntity) {
        when (task.entityType) {
            "memory" -> db.memoryDao().markConflict(task.entityId)
            "diary" -> db.diaryDao().markConflict(task.entityId)
            else -> Unit
        }
    }

    private fun describe(r: RemoteResult<*>): String = when (r) {
        is RemoteResult.Success -> "ok"
        is RemoteResult.NotConfigured -> r.message
        is RemoteResult.Network -> "network:" + r.message
        is RemoteResult.Unauthorized -> "unauthorized"
        is RemoteResult.Forbidden -> "forbidden"
        is RemoteResult.Conflict -> "conflict"
        is RemoteResult.BadRequest -> "bad_request:" + r.message
        is RemoteResult.Server -> "server:" + r.message
    }
}

data class SyncReport(
    val pushed: Int = 0,
    val pulled: Int = 0,
    val conflicts: Int = 0,
    val failed: Int = 0,
    val skipped: Boolean = false,
    val message: String = ""
)
