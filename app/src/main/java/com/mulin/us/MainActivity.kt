package com.mulin.us

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.mulin.us.data.sync.ForegroundSyncPoller
import com.mulin.us.ui.navigation.YuniNavigation
import com.mulin.us.ui.theme.YuniTheme

/**
 * 唯一的 Activity（单 Activity + Compose 导航）。
 *
 * 生命周期里只做两件事：
 * - onStart：开前台轮询（60 秒一次）+ 立刻同步一次 —— 回到前台最该做的是"拉对方的更新"；
 * - onStop：停轮询。后台绝不留定时器，省电也省资源点。
 */
class MainActivity : ComponentActivity() {

    private lateinit var poller: ForegroundSyncPoller

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as YuniApplication).container
        poller = ForegroundSyncPoller(container.syncCoordinator)

        setContent {
            YuniTheme {
                YuniNavigation(container)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val container = (application as YuniApplication).container
        poller.start(lifecycleScope, ForegroundSyncPoller.DEFAULT_INTERVAL_MS)
        if (container.syncRepository.cloudAvailable) {
            container.syncScheduler.requestImmediate("app-foreground")
        }
    }

    override fun onStop() {
        super.onStop()
        poller.stop()
    }
}
