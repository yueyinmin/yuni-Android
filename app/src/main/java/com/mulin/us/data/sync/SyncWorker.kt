package com.mulin.us.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mulin.us.YuniApplication
import com.mulin.us.core.SafeLog
import java.util.concurrent.TimeUnit

/**
 * 同步 Worker。
 *
 * 三重防重复（工单要求"重复任务不能造成重复数据"）：
 * 1. enqueueUniqueWork + ExistingWorkPolicy.KEEP —— 同一时刻只有一个任务在排队；
 * 2. SyncCoordinator 内部 Mutex —— 即使真有两条进来也串行执行；
 * 3. 云端 client_mutation_id 唯一约束 —— 最后一道防线。
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? YuniApplication
            ?: return Result.failure()
        return try {
            val report = app.container.syncCoordinator.runOnce(reason = "workmanager")
            when {
                report.skipped -> Result.success()          // 本地模式：什么也没做，不算失败
                report.failed > 0 -> Result.retry()         // 有失败：交给退避策略
                else -> Result.success()
            }
        } catch (e: Exception) {
            SafeLog.e("SyncWorker", "unexpected failure", e)
            if (runAttemptCount >= MAX_ATTEMPTS) Result.failure() else Result.retry()
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 5
    }
}

/**
 * 调度器：只负责"什么时候跑"，不负责"跑什么"。
 */
class SyncScheduler(private val context: Context) {

    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    /**
     * 兜底周期任务：系统最小值 15 分钟。
     * 注意：国产 ROM 会把这个压得更稀，所以不能把"及时性"押在它身上。
     */
    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        SafeLog.i("SyncScheduler", "periodic sync scheduled (15min)")
    }

    /** 立即同步一次：联网、App 回前台、用户手动刷新都走这里 */
    fun requestImmediate(reason: String) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(reason)
            .build()

        workManager.enqueueUniqueWork(ONE_SHOT_NAME, ExistingWorkPolicy.KEEP, request)
        SafeLog.d("SyncScheduler", "immediate sync requested: $reason")
    }

    fun cancelAll() {
        workManager.cancelUniqueWork(ONE_SHOT_NAME)
        workManager.cancelUniqueWork(PERIODIC_NAME)
    }

    private companion object {
        const val PERIODIC_NAME = "yuni_sync_periodic"
        const val ONE_SHOT_NAME = "yuni_sync_once"
    }
}
