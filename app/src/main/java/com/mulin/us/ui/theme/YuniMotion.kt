package com.mulin.us.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * 动效常量 —— 移植自轻记 v1.1.3 的 `com.qingji.app.ui.theme.Motion`。
 *
 * 轻记源码不在归档里，但参数从它自己的 patch 脚本原样提取：
 * - `Motion.Fast = 150ms`（qj_patch2.py 注释明写「取消选中走 150ms tween」）
 * - `Motion.smooth() = spring(0.85f, 900f)`（qj_patch4/5.py 指示器最终值）
 * - 选中弹跳 = `spring(MediumBouncy, StiffnessMedium)`，目标缩放 1.12
 * - 底栏进出 = `slideInVertically(tween(Normal, FastOutSlowInEasing)) + fadeIn(tween(Fast))`
 *
 * Normal 取 Material 常用的 300ms（轻记脚本只引用符号，未内联数值）。
 * 统一放这里，避免动效时长散落在各个页面里各写各的。
 */
object YuniMotion {
    /** 快：颜色、透明度、取消选中的缩回 */
    const val Fast = 150
    /** 常规：位移、底栏进出、页面转场 */
    const val Normal = 300
    /** 慢：仅在需要强调的场合使用 */
    const val Slow = 600

    /** 指示器滑动：轻记最终调参 spring(0.85f, 900f) */
    fun <T> smooth(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.85f, stiffness = 900f)

    /** 快速渐变 */
    fun <T> fast(): FiniteAnimationSpec<T> = tween(durationMillis = Fast, easing = FastOutSlowInEasing)

    /** 常规位移 */
    fun <T> normal(): FiniteAnimationSpec<T> = tween(durationMillis = Normal, easing = FastOutSlowInEasing)

    /** 选中弹跳：先冲过 1.12 再回落 */
    fun <T> bouncy(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
}
