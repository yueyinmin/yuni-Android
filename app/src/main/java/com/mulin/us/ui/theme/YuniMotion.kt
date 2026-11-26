package com.mulin.us.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

/**
 * 动效 token —— 数值照抄轻记源码 `ui/theme/Motion.kt`（完整源码在
 * `C:/qingji-git/QingJi-Android/`，不是交接包截图或旧 patch 脚本）。
 *
 * 14:50 修正：之前从 patch 脚本取的值有几处是轻记**已废弃的旧参数**：
 * - Normal 300 → 真值 220；Slow 600 → 320
 * - bouncy 用 MediumBouncy → 真值 spring(0.55, StiffnessMediumLow)
 * - smooth 用 0.85/900 → 真值 spring(0.9, StiffnessMediumLow)
 *   （0.85/900 是经典款 BottomBar 指示器，悬浮款 Dock 用另一套）
 * - **页面转场**：轻记当前是「纯淡入淡出 150ms 线性、零位移」，
 *   我上轮做的水平滑入是它在 `tabfix` 注释里明确废弃的旧方案。
 */
object YuniMotion {
    const val Instant = 90
    /** 快：颜色、透明度、页面淡变 */
    const val Fast = 150
    /** 常规：位移、尺寸 */
    const val Normal = 220
    const val Slow = 320

    /** 入场错开步长与上限（列表卡片依次淡入） */
    const val StaggerStep = 20L
    const val MaxStaggerIndex = 4

    /** 减速（进场） */
    val decelerate = LinearOutSlowInEasing
    /** 加速（退场） */
    val accelerate = FastOutLinearInEasing

    fun <T> fast(): FiniteAnimationSpec<T> = tween(durationMillis = Fast, easing = decelerate)
    fun <T> normal(): FiniteAnimationSpec<T> = tween(durationMillis = Normal, easing = decelerate)

    /** 带回一点弹性的弹簧：Tab 选中、按钮回弹 */
    fun <T> bouncy(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.55f,
        stiffness = Spring.StiffnessMediumLow
    )

    /** 平滑弹簧：尺寸/位置这类不该弹的地方 */
    fun <T> smooth(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.9f,
        stiffness = Spring.StiffnessMediumLow
    )

    /**
     * 页面转场：轻记 `tabfix` 后的终版——进出同为 150ms 线性淡变、
     * 零位移、零缩放。旧的水平滑入会造成两页错位重影，已废弃。
     */
    private val pageFade = LinearEasing
    val pageEnter: EnterTransition = fadeIn(tween(durationMillis = Fast, easing = pageFade))
    val pageExit: ExitTransition = fadeOut(tween(durationMillis = Fast, easing = pageFade))
    val popEnter: EnterTransition = pageEnter
    val popExit: ExitTransition = pageExit
}
