package com.mulin.us.data.sync

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 同步状态的内存持有者。
 *
 * 为什么不存数据库：
 * "上次同步时间/上次结果"只在当前这次打开 App 时有意义，
 * 存下来反而要处理"三天前的失败提示还挂着"这种噪声。
 */
object SyncStateStore {
    val lastSyncAt = MutableStateFlow(0L)
    val lastResult = MutableStateFlow("尚未同步")
}
