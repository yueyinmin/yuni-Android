package com.mulin.us.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 导航结构单测（B0.1 T04）。
 *
 * 注意：这里测的是"路由表 + 底栏显隐规则"，
 * 不是真的把 Compose 页面渲染出来点一遍（那需要仪器测试 / 真机）。
 * 工单验收里"导航测试"这项目前只覆盖到这里，
 * 真机点击验证仍是待办，不冒充已通过。
 */
class YuniNavigationTest {

    @Test
    fun `五个Tab顺序和名称完全符合K3`() {
        assertEquals(
            listOf("首页", "回忆", "互动", "信箱", "我们"),
            YuniNavSpec.tabs.map { it.label }
        )
        assertEquals(
            listOf(
                YuniRoutes.HOME,
                YuniRoutes.MEMORIES,
                YuniRoutes.INTERACTION,
                YuniRoutes.MAILBOX,
                YuniRoutes.US
            ),
            YuniNavSpec.tabs.map { it.route }
        )
    }

    @Test
    fun `路由不能重复`() {
        val routes = YuniNavSpec.tabs.map { it.route }
        assertEquals("有重复路由会导致页面互相覆盖", routes.size, routes.toSet().size)
    }

    @Test
    fun `日记挂在回忆下_愿望挂在我们下`() {
        assertTrue("日记路径必须以 memories 开头", YuniRoutes.DIARY.startsWith("memories/"))
        assertTrue("愿望路径必须以 us 开头", YuniRoutes.WISHES.startsWith("us/"))
    }

    @Test
    fun `底栏只在顶级Tab显示`() {
        YuniNavSpec.topLevelRoutes.forEach { route ->
            assertTrue("$route 是顶级 Tab，必须显示底栏", YuniNavSpec.shouldShowBottomBar(route))
        }
        assertFalse("日记是二级页，不显示底栏", YuniNavSpec.shouldShowBottomBar(YuniRoutes.DIARY))
        assertFalse("愿望是二级页，不显示底栏", YuniNavSpec.shouldShowBottomBar(YuniRoutes.WISHES))
        assertFalse("未知路由不显示底栏", YuniNavSpec.shouldShowBottomBar("unknown"))
        assertFalse("空路由不显示底栏", YuniNavSpec.shouldShowBottomBar(null))
    }
}
