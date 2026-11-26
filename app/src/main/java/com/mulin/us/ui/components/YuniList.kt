package com.mulin.us.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 轻记 `ui/components/UnifiedUi.kt` 的语义色图标底移植。
 * 七种固定色：蓝=数据、绿=健康/隐私、橙=记录、紫=智能/外观、粉=偏好、青=同步、灰=系统。
 */
data class YuniTone(val icon: Color, val container: Color)

object YuniTones {
    val Green = YuniTone(Color(0xFF299879), Color(0xFFE7F4EE))
    val Blue = YuniTone(Color(0xFF528FE0), Color(0xFFE9F1FC))
    val Orange = YuniTone(Color(0xFFE5A044), Color(0xFFFBF1E2))
    val Purple = YuniTone(Color(0xFF9276D8), Color(0xFFF0ECFA))
    val Pink = YuniTone(Color(0xFFDB83A5), Color(0xFFF9EBF1))
    val Teal = YuniTone(Color(0xFF4BAEB0), Color(0xFFE7F4F3))
    val Gray = YuniTone(Color(0xFF87949A), Color(0xFFEDF0F1))
}

/** 38dp 圆角图标底 + 20dp 线性图标（轻记 FeatureIcon 真值） */
@Composable
fun YuniFeatureIcon(
    icon: ImageVector,
    tone: YuniTone,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tone.container),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tone.icon,
            modifier = Modifier.size(20.dp))
    }
}

/** 分组标题：13sp 次文字，卡片外左对齐（轻记 GroupHeader 真值） */
@Composable
fun YuniGroupHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp, top = 14.dp, bottom = 6.dp)
    )
}

/** 行间分割线：左侧留 64dp 对齐标题（轻记 GroupDivider 真值） */
@Composable
fun YuniGroupDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 64.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
    )
}

/**
 * 统一列表行：38dp 彩色图标底 + 14sp 标题 + 可选右侧状态 + 箭头，最小高 60dp。
 * 轻记 SettingsRow 真值结构。
 */
@Composable
fun YuniSettingsRow(
    title: String,
    icon: ImageVector,
    tone: YuniTone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String = "",
    subtitle: String? = null,
    showArrow: Boolean = true,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        YuniFeatureIcon(icon = icon, tone = tone)
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(text = subtitle, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing.isNotBlank()) {
            // 灰字直接压在白卡上显得脏，统一进浅底小胶囊，层次更清楚
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = trailing, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (showArrow) {
            Spacer(Modifier.size(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
