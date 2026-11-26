package com.mulin.us.data.sync

import com.mulin.us.core.common.SafeLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 前台增量同步轮询。
 *
 * 为什么是轮询而不是长连接：
 * CloudBase 的 PostgreSQL 实时订阅目前是 Private Alpha，官方明示不得用于生产；
 * 旧的文档数据库 watch 在新环境已不存在。所以"实时"只能靠定时拉。
 *
 * 规则（来自工单 B0-7）：
 * - App 回前台：立刻同步一次（由调用方 requestImmediate 触发）
 * - 首页可见：默认 60 秒
 * - 双人互动页：可临时切到 20 秒
 * - 离开页面 / 进入后台：stop()
 *
 * 全 App 共用一个实例（在 MainActivity 持有），不允许每个页面各起一个轮询。
 */
class ForegroundSyncPoller(private val coordinator: SyncCoordinator) {

    private var job: Job? = null

    val isRunning: Boolean get() = job?.isActive == true

    fun start(scope: CoroutineScope, intervalMs: Long = DEFAULT_INTERVAL_MS) {
        stop()
        job = scope.launch {
            // 一进来先同步一次：回到前台时最该做的事就是"把对方的更新拉下来"
            coordinator.runOnce(reason = "foreground-resume")
            while (isActive) {
                delay(intervalMs)
                if (!isActive) break
                coordinator.runOnce(reason = "poll-${intervalMs / 1000}s")
            }
        }
        SafeLog.i("Poller", "started interval=${intervalMs}ms")
    }

    fun stop() {
        if (job != null) {
            job?.cancel()
            job = null
            SafeLog.i("Poller", "stopped")
        }
    }

    companion object {
        /** 首页 / 列表页 */
        const val DEFAULT_INTERVAL_MS = 60_000L
        /** 正在双人互动（问答、悄悄话）时可临时缩短 */
        const val INTERACTIVE_INTERVAL_MS = 20_000L
    }
}
