package com.mulin.us.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mulin.us.ui.theme.YuniMotion
import kotlin.math.abs

/**
 * 轻记悬浮底栏 v13 dockref 终版移植（`ui/navigation/BottomBar.kt`）。
 *
 * 用户（轻记里"主人"）当时的明确要求，源码注释逐条留着：
 * 1. 选中阴影是一条**共享胶囊**，从旧槽滑到新槽，不再每 Tab 自画（会瞬现/瞬失）；
 * 2. **选中的槽位动态变宽**（1.5:1 权重），胶囊才放得下图标+文字，未选中收窄让位；
 * 3. 图标必须**居中在选中胶囊里**（文字行高钉死 16sp，否则中文默认行高把图标顶上去）；
 * 4. 归位用**低振荡弹簧**（阻尼 0.9 / 380），不要会来回晃的 0.5；
 * 5. **去掉涟漪**，选中反馈 = 胶囊溜过来 + 图标轻弹，够了；
 * 6. 选中展开统一 200ms。
 *
 * 之前的"等宽槽 + 46×28 小胶囊只衬图标 + 右侧 CTA"是轻记 v11 之前淘汰的方案，
 * 用户说"当前选中还是太丑"——就是因为这个。现在按 v13 重写，并去掉 CTA。
 */
private val DockPillShape = RoundedCornerShape(percent = 50)
private val DockHeight = 62.dp
private val DockHorizontalPadding = 10.dp
private val DockHorizontalPaddingNarrow = 6.dp

/** 选中胶囊：46dp 高，宽度跟槽位走、封顶 82dp（轻记 PillHeight/PillMaxWidth） */
private val PillHeight = 42.dp
private val PillMaxWidth = 82.dp
private val PillHorizontalPadding = 8.dp
private val PillIconSize = 20.dp
private val NormalIconSize = 21.dp
private val PillIconTextGap = 5.dp

/** 选中槽位 1.5:1；窄屏保底选中槽位 79dp、权重上限 2.8 */
private const val SelectedTabWeight = 1.5f
private const val MaxSelectedTabWeight = 2.8f
private const val PillTargetSlot = 79f
private const val SelectMotionMs = 200

/** 线性凸起：权重总和对任意 progress 恒定 → 底栏宽度不呼吸 */
private fun weightAt(progress: Float, i: Int, selectedWeight: Float): Float {
    val d = abs(progress - i).coerceAtMost(1f)
    return 1f + (selectedWeight - 1f) * (1f - d)
}

private fun selectedTabWeightFor(tabArea: Dp, normalCount: Int): Float {
    if (normalCount <= 0) return SelectedTabWeight
    if (tabArea.value <= PillTargetSlot) return MaxSelectedTabWeight
    val need = normalCount * PillTargetSlot / (tabArea.value - PillTargetSlot)
    return need.coerceIn(SelectedTabWeight, MaxSelectedTabWeight)
}

/** 低振荡归位弹簧（主人要求：不要会晃的 0.5） */
private fun dockSnapSpec() = spring<Float>(dampingRatio = 0.9f, stiffness = 380f)

@Composable
fun YuniDock(
    tabs: List<YuniTabSpec>,
    currentRoute: String?,
    onTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 22.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(DockHeight)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(31.dp),
                    ambientColor = Color(0x3393827E),
                    spotColor = Color(0x3393827E)
                )
                .clip(RoundedCornerShape(31.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            FloatingBarSlots(tabs, currentRoute, onTab)
        }
    }
}

@Composable
private fun FloatingBarSlots(
    tabs: List<YuniTabSpec>,
    currentRoute: String?,
    onTab: (String) -> Unit
) {
    val density = LocalDensity.current
    val pillBg = Color(0xFFFBE9E8)
    val pillContent = Color(0xFFE9A0A5)
    val activeIndex = tabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val narrow = maxWidth < 360.dp
        val hPad = if (narrow) DockHorizontalPaddingNarrow else DockHorizontalPadding

        // 无 CTA：所有宽度都分给 Tab
        val tabArea = maxWidth - hPad * 2
        val selWeight = selectedTabWeightFor(tabArea, tabs.size - 1)
        val slotW = tabArea * selWeight / (selWeight + (tabs.size - 1))
        val pillW = minOf(PillMaxWidth, slotW - 2.dp)
            .coerceAtLeast(minOf(
                PillIconSize + PillIconTextGap + 26.dp + PillHorizontalPadding,
                slotW
            ))

        val tabAreaPx = with(density) { tabArea.toPx() }
        val pillWPx = with(density) { pillW.toPx() }

        // 槽位几何：完全由进度算，不依赖实际测量 → 首帧胶囊直接落位，不从 0 飞过来
        fun widthOf(i: Int, progress: Float): Float {
            var sum = 0f
            for (k in tabs.indices) sum += weightAt(progress, k, selWeight)
            if (sum <= 0f) return 0f
            return tabAreaPx * weightAt(progress, i, selWeight) / sum
        }
        fun centerOf(i: Int, progress: Float): Float {
            var x = 0f
            for (k in 0 until i) x += widthOf(k, progress)
            return x + widthOf(i, progress) / 2f
        }

        val restCenter = centerOf(activeIndex, activeIndex.toFloat())

        // 布局进度：200ms 动画；只驱动 measure，不触发重组
        val selIndex = remember { Animatable(activeIndex.toFloat()) }
        LaunchedEffect(activeIndex) {
            selIndex.animateTo(activeIndex.toFloat(), tween(SelectMotionMs))
        }
        // 胶囊中心：低振荡弹簧归位（主人要求）
        val pillCenter = remember { mutableFloatStateOf(restCenter) }
        LaunchedEffect(restCenter) {
            if (pillCenter.floatValue == restCenter) return@LaunchedEffect
            animate(pillCenter.floatValue, restCenter, animationSpec = dockSnapSpec()) { value, _ ->
                pillCenter.floatValue = value
            }
        }

        // ---------- 1. 共享胶囊层（宽胶囊含图标+文字的槽底）----------
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = hPad)) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(pillW)
                    .height(PillHeight)
                    .graphicsLayer {
                        translationX = pillCenter.floatValue - pillWPx / 2f
                    }
                    .clip(DockPillShape)
                    .background(pillBg)
            )
        }

        // ---------- 2. Tab 层：自定义 Layout 按权重分配宽度 ----------
        Layout(
            content = {
                for (tab in tabs) {
                    DockItem(
                        tab = tab,
                        highlighted = currentRoute == tab.route,
                        tint = pillContent,
                        onClick = { onTab(tab.route) }
                    )
                }
            },
            modifier = Modifier.fillMaxSize().padding(horizontal = hPad)
        ) { measurables, constraints ->
            val progress = selIndex.value
            val totalWeight = tabs.indices.sumOf { weightAt(progress, it, selWeight).toDouble() }.toFloat()
            // 关键：按权重宽度测量每个 Tab（不是全宽），Column fillMaxWidth 后图标才真正居中
            val widths = tabs.indices.map { i ->
                (constraints.maxWidth * weightAt(progress, i, selWeight) / totalWeight).toInt()
            }
            val placeables = measurables.zip(widths).map { (m, w) ->
                m.measure(constraints.copy(minWidth = w, maxWidth = w))
            }
            layout(constraints.maxWidth, constraints.maxHeight) {
                var x = 0f
                placeables.forEachIndexed { i, placeable ->
                    placeable.place(x.toInt(), (constraints.maxHeight - placeable.height) / 2)
                    x += widths[i]
                }
            }
        }
    }
}

@Composable
private fun DockItem(
    tab: YuniTabSpec,
    highlighted: Boolean,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val itemTint by animateColorAsState(
        targetValue = if (highlighted) tint else Color(0xFF8C7A75),
        animationSpec = YuniMotion.fast(),
        label = "dock-tint"
    )
    val emphasis by animateFloatAsState(
        targetValue = if (highlighted) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 420f),
        label = "dock-emphasis"
    )

    // 轻记悬浮款 FloatingTabItem：横排 Row(Icon + 选中时展开的文字)，未选中只有图标。
    // 去涟漪（indication=null）：选中反馈 = 胶囊滑过来 + 图标轻弹。
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = itemTint,
                modifier = Modifier
                    .size(NormalIconSize)
                    .graphicsLayer { scaleX = emphasis; scaleY = emphasis }
            )
            AnimatedVisibility(
                visible = highlighted,
                enter = fadeIn(tween(YuniMotion.Fast)) + expandHorizontally(tween(SelectMotionMs)),
                exit = fadeOut(tween(180)) + shrinkHorizontally(tween(180))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(PillIconTextGap))
                    Text(
                        text = tab.label,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = itemTint,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
