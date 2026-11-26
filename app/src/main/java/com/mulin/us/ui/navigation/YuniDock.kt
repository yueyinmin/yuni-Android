package com.mulin.us.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mulin.us.ui.theme.YuniMotion

/**
 * 轻记悬浮底栏（QingJiFloatingBar）源码真值移植：
 *
 * - 左右 16dp / 底部 22dp 悬空；shadow 4dp、31dp 圆角、纯白底
 * - 五个 Tab **横排**（图标 20dp + 文字 13sp），选中时文字淡入展开、图标放大回弹
 * - 共享胶囊只衬图标（46×28，不是整槽大块），从旧槽滑到新槽
 * - 右侧深胭脂 CTA 胶囊「+ 记一条」——轻记「+ 记一笔」对应到予你就是写回忆
 *
 * 之前竖排版本是从经典款 BottomBar 的旧 patch 抄的，悬浮款实际是横排 + CTA。
 */
private val DockPillShape = RoundedCornerShape(999.dp)
private val DockHeight = 56.dp
private val PillWidth = 46.dp
private val PillHeight = 28.dp
private val CtaWidth = 104.dp

@Composable
fun YuniDock(
    tabs: List<YuniTabSpec>,
    currentRoute: String?,
    onTab: (String) -> Unit,
    onQuickRecord: () -> Unit,
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
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(31.dp))
                .clip(RoundedCornerShape(31.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val ctaWidth: Dp = if (maxWidth < 340.dp) 92.dp else CtaWidth
                val tabArea = maxWidth - ctaWidth
                val slotWidth = tabArea / tabs.size.coerceAtLeast(1)
                val activeSlot = tabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

                // 共享胶囊：只衬图标，位置 = 槽中心 - 胶囊宽/2
                val animatedSlot by animateFloatAsState(
                    targetValue = activeSlot.toFloat(),
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 900f),
                    label = "dock-indicator"
                )
                Box(
                    modifier = Modifier
                        .offset(
                            x = slotWidth * animatedSlot + (slotWidth - PillWidth) / 2,
                            y = (DockHeight - PillHeight) / 2
                        )
                        .width(PillWidth)
                        .height(PillHeight)
                        .clip(DockPillShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.width(tabArea).fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEach { tab ->
                            DockItem(
                                tab = tab,
                                selected = currentRoute == tab.route,
                                onClick = { onTab(tab.route) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    QuickRecordButton(width = ctaWidth, onClick = onQuickRecord)
                }
            }
        }
    }
}

@Composable
private fun RowScope.DockItem(
    tab: YuniTabSpec,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = YuniMotion.fast(),
        label = "dock-tint"
    )
    // 选中图标放大回弹（轻记悬浮款 spring 0.7/420；经典款才是 1.12）
    val emphasis by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 420f),
        label = "dock-emphasis"
    )

    Box(
        modifier = modifier.fillMaxSize().clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = tint,
                modifier = Modifier.size(20.dp).graphicsLayer {
                    scaleX = emphasis
                    scaleY = emphasis
                }
            )
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(tween(YuniMotion.Fast)) + expandHorizontally(tween(YuniMotion.Fast)),
                exit = fadeOut(tween(180)) + shrinkHorizontally(tween(180))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = tab.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tint,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** 右侧「+ 记一条」：深胭脂胶囊，轻记 AddPillButton 同款结构 */
@Composable
private fun QuickRecordButton(width: Dp, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .padding(vertical = 4.dp)
            .clip(DockPillShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "记一条",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "记一条",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 1
        )
    }
}
