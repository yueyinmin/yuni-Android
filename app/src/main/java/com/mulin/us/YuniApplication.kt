package com.mulin.us

import android.app.Application
import com.mulin.us.core.AppContainer
import com.mulin.us.core.common.SafeLog

/**
 * 予你 · Us 的 Application。
 *
 * 只做三件事，不做第四件：
 * 1. 建依赖容器；
 * 2. 恢复登录态（只把 uid 放回内存，不碰数据库）；
 * 3. 排上兜底周期同步。
 *
 * 注意：这里**没有**任何云端初始化代码（LeanCloud 时代那种 AVOSCloud.initialize 不存在了）。
 * 没配环境时容器注入的是本地替身，连 OkHttp 都不会被创建。
 */
class YuniApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.restoreSession()
        container.syncScheduler.schedulePeriodic()
        SafeLog.i("App", "started, cloud=${container.cloudStateText()}")
    }
}
