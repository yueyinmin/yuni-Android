package com.mulin.us.ui.navigation

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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mulin.us.core.AppContainer
import com.mulin.us.ui.YuniViewModel
import com.mulin.us.ui.YuniViewModelFactory
import com.mulin.us.ui.couple.UsScreen
import com.mulin.us.ui.couple.WishScreen
import com.mulin.us.ui.home.HomeScreen
import com.mulin.us.ui.interaction.InteractionScreen
import com.mulin.us.ui.mailbox.MailboxScreen
import com.mulin.us.ui.memories.DiaryScreen
import com.mulin.us.ui.memories.MemoryListScreen

/**
 * 路由表。
 *
 * 为什么集中放一处：
 * 路由是字符串，散落在各处写 "home" / "memories" 的话，改一个名字要全工程搜。
 * 而且拼错不会编译报错，只会在运行时"点了没反应"——最难查的一类问题。
 *
 * 二级页用 "父/子" 的写法，一眼能看出它属于哪个 Tab。
 */
object YuniRoutes {
    // ---- 五个顶级 Tab ----
    const val HOME = "home"
    const val MEMORIES = "memories"
    const val INTERACTION = "interaction"
    const val MAILBOX = "mailbox"
    const val US = "us"

    // ---- 二级页面（不挂底栏）----
    /** 日记：原底部第 3 个 Tab，B0.1 T04 起归入「回忆」 */
    const val DIARY = "memories/diary"
    /** 愿望清单：原底部第 4 个 Tab，B0.1 T04 起归入「我们」 */
    const val WISHES = "us/wishes"
}

/**
 * 一个底部 Tab。
 *
 * glyph 用单个汉字而不是图标：B0 阶段不引入 material-icons-extended（约 3MB），
 * 用首字占位在功能上等价，等 B7 做视觉统一时再换成正式图标。
 */
private data class YuniTab(
    val route: String,
    val label: String,
    val glyph: String
)

private val yuniTabs = listOf(
    YuniTab(YuniRoutes.HOME, "首页", "首"),
    YuniTab(YuniRoutes.MEMORIES, "回忆", "忆"),
    YuniTab(YuniRoutes.INTERACTION, "互动", "动"),
    YuniTab(YuniRoutes.MAILBOX, "信箱", "信"),
    YuniTab(YuniRoutes.US, "我们", "我")
)

/**
 * 应用导航根。
 *
 * 结构（K3 确认的五 Tab）：
 *   首页 │ 回忆 │ 互动 │ 信箱 │ 我们
 *          └ 日记            └ 愿望清单
 *
 * 三条设计约束：
 * 1. 底栏**只在五个顶级 Tab 显示**。二级页靠顶部"← 返回"回退，
 *    避免"底栏 + 返回"两套导航同时出现把人绕晕（工单 7.3 第 5 条）。
 * 2. Tab 切换用 popUpTo(startDestination) + saveState/restoreState + launchSingleTop，
 *    这样来回切不会堆栈越积越深，也不会每次重建页面导致列表滚动位置丢失。
 * 3. 二级页**不吃**外层的 innerPadding，因为它自己就是一个 Scaffold，
 *    否则顶部会出现两层状态栏留白。
 */
@Composable
fun YuniNavigation(container: AppContainer) {
    val navController: NavHostController = rememberNavController()
    val vm: YuniViewModel = viewModel(factory = YuniViewModelFactory(container))

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val topLevelRoutes = yuniTabs.map { it.route }
    val showBottomBar = currentRoute in topLevelRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    yuniTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navController.switchTab(tab.route) },
                            icon = {
                                Text(text = tab.glyph, textAlign = TextAlign.Center)
                            },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = YuniRoutes.HOME
        ) {
            // ---------- 顶级 Tab ----------
            composable(YuniRoutes.HOME) {
                HomeScreen(vm = vm, modifier = Modifier.padding(innerPadding))
            }
            composable(YuniRoutes.MEMORIES) {
                MemoryListScreen(
                    vm = vm,
                    onOpenDiary = { navController.navigate(YuniRoutes.DIARY) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            composable(YuniRoutes.INTERACTION) {
                InteractionScreen(modifier = Modifier.padding(innerPadding))
            }
            composable(YuniRoutes.MAILBOX) {
                MailboxScreen(modifier = Modifier.padding(innerPadding))
            }
            composable(YuniRoutes.US) {
                UsScreen(
                    vm = vm,
                    onOpenWishes = { navController.navigate(YuniRoutes.WISHES) },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            // ---------- 二级页（自带 Scaffold，不吃 innerPadding）----------
            composable(YuniRoutes.DIARY) {
                DiaryScreen(vm = vm, onBack = { navController.popBackStack() })
            }
            composable(YuniRoutes.WISHES) {
                WishScreen(vm = vm, onBack = { navController.popBackStack() })
            }
        }
    }
}

/**
 * 切换 Tab 的标准动作。
 *
 * 四个参数每一个都有明确作用，少一个就出问题：
 * - popUpTo(起始页) { saveState = true }：切走时把当前 Tab 的界面状态存起来
 * - launchSingleTop = true：连点同一个 Tab 不会压出多个相同页面
 * - restoreState = true：切回来时恢复上次的滚动位置与输入内容
 * - 不设 inclusive：起始页本身要留在栈里，否则返回键会直接退出 App
 */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
