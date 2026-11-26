package com.mulin.us.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mulin.us.core.AppContainer
import com.mulin.us.ui.screen.DiaryScreen
import com.mulin.us.ui.screen.HomeScreen
import com.mulin.us.ui.screen.MemoryScreen
import com.mulin.us.ui.screen.MineScreen
import com.mulin.us.ui.screen.WishScreen

/**
 * 导航骨架。
 *
 * B0 只有五个空壳页 + 一个能用的自检页。
 * 明确不做：登录页、绑定页、回忆详情、问答页、信件页 —— 那些属于 B1~B3。
 */

private data class Tab(val route: String, val label: String, val subtitle: String)

private val tabs = listOf(
    Tab("home", "首页", "纪念日"),
    Tab("memory", "回忆", "照片"),
    Tab("diary", "日记", "私密/共同"),
    Tab("wish", "愿望", "清单"),
    Tab("mine", "我的", "自检")
)

@Composable
fun YuniApp(container: AppContainer) {
    val navController = rememberNavController()
    val vm: YuniViewModel = viewModel(factory = YuniViewModelFactory(container))
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "home"

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            // B0 不引入 material-icons 依赖，先用文字占位，省 3MB 体积
                            Text(
                                text = tab.label.take(1),
                                textAlign = TextAlign.Center
                            )
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(navController = navController, startDestination = "home") {
                composable("home") { HomeScreen(vm) }
                composable("memory") { MemoryScreen(vm) }
                composable("diary") { DiaryScreen(vm) }
                composable("wish") { WishScreen(vm) }
                composable("mine") { MineScreen(vm) }
            }
        }
    }
}
