package com.mulin.us.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mulin.us.R
import com.mulin.us.ui.components.YuniAvatar
import com.mulin.us.ui.theme.UsSpacing
import com.mulin.us.ui.theme.YuniWaveShape
import java.time.LocalDate

data class MemoryPreviewUiModel(
    val id: String,
    val dateText: String,
    val title: String,
    val summary: String,
    val coverUrl: String?
)

data class HomeUiState(
    val quote: String = "与你在一起，\n就是最好的时光",
    val myInitial: String = "",
    val myNickname: String = "",
    val partnerAvatarUrl: String? = null,
    val isPartnerBound: Boolean = false,
    val loveStartDate: LocalDate? = null,
    val daysTogether: Long = 0L,
    val todayDateText: String = "",
    val todayWeekdayText: String = "",
    val dailyNote: String = "今天也要一起呀",
    val nextMemory: MemoryPreviewUiModel? = null
)

@Composable
fun YuniGlassCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = HomeTokens.AnniversaryShape,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = HomeTokens.CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, HomeTokens.CardBorder),
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(6.dp)) { content() }
    }
}

@Composable
fun HomeHeaderBar(
    myInitial: String,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f).height(72.dp)) {
            StarTrailDecoration(modifier = Modifier.fillMaxSize(), showStars = true)
            Column(modifier = Modifier.padding(start = 4.dp, top = 2.dp)) {
                Text("予你", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = HomeTokens.TextPrimary,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif)
                Text("Us · 两个人的私密空间", fontSize = 13.sp, color = HomeTokens.TextSecondary)
            }
        }
        Surface(
            shape = CircleShape,
            color = HomeTokens.PillBlush,
            onClick = onSettingsClick
        ) {
            Row(
                modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_settings),
                    contentDescription = null,
                    tint = HomeTokens.AccentRose,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text("设置", fontSize = 14.sp, color = HomeTokens.TextPrimary)
            }
        }
    }
}

@Composable
fun HeroCoupleBanner(
    quote: String,
    myAvatarPath: String?,
    myInitial: String,
    myNickname: String,
    partnerBound: Boolean,
    onInvitePartner: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2.05f)
            .heightIn(min = 160.dp, max = 220.dp)
            .clip(YuniWaveShape)
    ) {
        Image(
            painter = painterResource(R.drawable.bg_home_hero),
            contentDescription = "星空下的旅行者与玻璃罩玫瑰",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    listOf(
                        HomeTokens.Surface.copy(alpha = 0.72f),
                        HomeTokens.Surface.copy(alpha = 0.30f),
                        Color.Transparent
                    )
                )
            )
        )
        Column(modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp)) {
            Text(
                "「 $quote 」",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = HomeTokens.TextPrimary,
                maxLines = 3
            )
            Spacer(Modifier.weight(1f))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    YuniAvatar(imagePath = myAvatarPath, label = myInitial, contentDescription = "我的头像")
                    Spacer(Modifier.width(8.dp))
                    Text("&", color = HomeTokens.TextSecondary)
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.clickable { if (!partnerBound) onInvitePartner() }) {
                        YuniAvatar(
                            imagePath = null,
                            label = if (partnerBound) "TA" else "?",
                            soft = !partnerBound,
                            contentDescription = if (partnerBound) "另一半头像" else "邀请另一半"
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (partnerBound) myNickname else "等待另一半加入",
                    fontSize = 13.sp,
                    color = HomeTokens.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
fun AnniversaryCounterCard(
    days: Long?,
    startDate: String?,
    weekday: String,
    onEditDate: () -> Unit,
    modifier: Modifier = Modifier
) {
    YuniGlassCard(modifier) {
        Box {
            StarTrailDecoration(
                modifier = Modifier.align(Alignment.TopEnd).size(64.dp),
                showStars = true
            )
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("我们已经在一起", fontSize = 15.sp, color = HomeTokens.TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            if (days == null) "—" else days.toString(),
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            color = HomeTokens.ReferenceRose
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("天", fontSize = 18.sp, color = HomeTokens.TextPrimary,
                            modifier = Modifier.padding(bottom = 16.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_nav_calendar),
                            contentDescription = null,
                            tint = HomeTokens.AccentRose,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            if (startDate == null) "点这里写下恋爱开始日" else "相恋 · 自 $startDate",
                            fontSize = 12.sp,
                            color = HomeTokens.AccentRose,
                            modifier = Modifier.clickable { onEditDate() }
                        )
                    }
                }
                VerticalDivider(
                    thickness = 1.dp,
                    color = HomeTokens.TextPrimary.copy(alpha = 0.10f),
                    modifier = Modifier.height(86.dp).padding(horizontal = 12.dp)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "一起的日子",
                        fontSize = 12.sp,
                        color = HomeTokens.AccentRose,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(HomeTokens.PillBlush)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(startDate ?: "未设置", fontSize = 14.sp, color = HomeTokens.TextPrimary)
                    Text(weekday, fontSize = 12.sp, color = HomeTokens.TextSecondary)
                }
            }
        }
    }
}

@Composable
fun DailyInteractionCard(
    dailyNote: String,
    onOpenInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    YuniGlassCard(modifier, shape = HomeTokens.InteractionShape) {
        Row(modifier = Modifier.fillMaxWidth().height(26.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_nav_interaction),
                contentDescription = null,
                tint = HomeTokens.AccentRose,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("今日互动", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = HomeTokens.TextPrimary)
            Spacer(Modifier.weight(1f))
            Text(
                "每日问答 · 悄悄话 ›",
                fontSize = 11.sp,
                color = HomeTokens.TextSecondary,
                maxLines = 1,
                modifier = Modifier.clickable { onOpenInteraction() }.padding(start = 4.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Box {
            // 参考图右侧是整片水彩云+玻璃玫瑰，而非孤立小图
            Image(
                painter = painterResource(R.drawable.img_rose_cloche),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(56.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.92f
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 128.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(HomeTokens.PillBlush)
                    .clickable { onOpenInteraction() }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_nav_interaction),
                        contentDescription = null,
                        tint = HomeTokens.AccentRose,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(dailyNote, fontSize = 14.sp, color = HomeTokens.AccentRose,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun MemoriesPreviewCard(
    memory: MemoryPreviewUiModel?,
    onOpenMemory: (MemoryPreviewUiModel) -> Unit,
    onOpenAll: () -> Unit,
    onAddMemory: () -> Unit,
    modifier: Modifier = Modifier
) {
    YuniGlassCard(modifier, shape = HomeTokens.MemoriesShape) {
        Row(modifier = Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_action_photo),
                contentDescription = null,
                tint = HomeTokens.AccentRose,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("我们的回忆", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFF3E332F))
            Spacer(Modifier.weight(1f))
            Text("查看全部 ›", fontSize = 12.sp, color = Color(0xFF6B5A54),
                modifier = Modifier.clickable { onOpenAll() })
        }
        Spacer(Modifier.height(16.dp))
        if (memory == null) {
            Text(
                "添加你们的第一个纪念日",
                fontSize = 13.sp,
                color = HomeTokens.TextSecondary,
                modifier = Modifier.clickable { onAddMemory() }.padding(vertical = 12.dp)
            )
        } else {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.78f),
                modifier = Modifier.clickable { onOpenMemory(memory) }
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = memory.coverUrl ?: R.drawable.bg_home_hero,
                        contentDescription = null,
                        modifier = Modifier.size(width = 88.dp, height = 48.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.bg_home_hero),
                        error = painterResource(R.drawable.bg_home_hero)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(memory.dateText, fontSize = 11.sp, color = HomeTokens.TextSecondary, maxLines = 1)
                        Spacer(Modifier.height(3.dp))
                        Text(memory.title, fontSize = 14.sp, color = HomeTokens.TextPrimary, maxLines = 1)
                        Spacer(Modifier.height(3.dp))
                        Text(memory.summary.ifBlank { "还没有写下描述" },
                            fontSize = 11.sp, color = HomeTokens.TextSecondary, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_action_arrow),
                        contentDescription = null,
                        tint = HomeTokens.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
