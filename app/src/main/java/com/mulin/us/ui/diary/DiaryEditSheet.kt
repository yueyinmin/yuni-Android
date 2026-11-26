package com.mulin.us.ui.diary

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mulin.us.ui.components.YuniCard
import java.time.LocalDate

/**
 * 写日记 / 写回忆 —— 轻记 DiaryEditScreen **全屏页**（不是弹窗！）。
 *
 * 结构：顶部返回栏（56dp 白底）→ 浅灰背景内容区滚动（日期/星级/文字/照片）
 *      → 底部固定保存栏。按用户要求去掉「关联当日体重」。
 *
 * 之前用 AlertDialog 粉底套白卡：卡片被截断、红框焦点、层次混乱，就是"丑"的根源。
 */
@Composable
fun DiaryEditScreen(
    title: String,
    onBack: () -> Unit,
    onSave: (date: LocalDate, rating: Int, content: String, photos: List<Uri>) -> Unit,
    initialDate: LocalDate = LocalDate.now()
) {
    var dateText by remember { mutableStateOf(initialDate.toString()) }
    var rating by remember { mutableStateOf(3) }
    var content by remember { mutableStateOf("") }
    var photos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var error by remember { mutableStateOf("") }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val remain = 9 - photos.size
        if (remain <= 0) {
            error = "最多 9 张照片，先删掉几张再加"
        } else {
            photos = photos + uris.take(remain)
            error = ""
        }
    }

    val rose = MaterialTheme.colorScheme.primary
    val roseSoft = MaterialTheme.colorScheme.primaryContainer
    val sub = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // ---------- 顶部返回栏（轻记 PageTopBar）----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
            }
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface)
        }

        // ---------- 内容区（可滚动，weight 给剩余高度）----------
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState(), enabled = false)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(6.dp))
            YuniCard {
                Column(modifier = Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = dateText, onValueChange = { dateText = it },
                    label = { Text("日期（yyyy-MM-dd）") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                Text("今天给自己打几分", fontSize = 13.sp, color = sub)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..5).forEach { star ->
                        val selected = star <= rating
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) roseSoft else MaterialTheme.colorScheme.surface)
                                .clickable { rating = star }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$star★", fontSize = 14.sp,
                                color = if (selected) rose else sub)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = content, onValueChange = { content = it },
                    label = { Text("写点什么") },
                    placeholder = { Text("今天发生了什么…") },
                    minLines = 5, modifier = Modifier.fillMaxWidth()
                )
                }
            }

            YuniCard {
                Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("照片（最多 9 张）", fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("照片只存在这个 App 里", fontSize = 11.sp, color = sub)
                    }
                    Text("+ 添加", fontSize = 13.sp, color = rose,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(roseSoft)
                            .clickable { pick.launch("image/*") }
                            .padding(horizontal = 10.dp, vertical = 6.dp))
                }
                if (photos.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    photos.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { uri ->
                                Box(modifier = Modifier.weight(1f)) {
                                    AsyncImage(
                                        model = uri, contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .height(84.dp)
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(roseSoft)
                                    )
                                    Text("×", fontSize = 16.sp, color = androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(androidx.compose.ui.graphics.Color(0x99000000))
                                            .clickable { photos = photos - uri }
                                            .padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                }
            }

            if (error.isNotBlank()) {
                Text(error, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
        }

        // ---------- 底部固定保存栏 ----------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 24.dp, vertical = 14.dp)
                .clickable {
                    val date = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull()
                    if (date == null) {
                        error = "日期格式应为 yyyy-MM-dd"
                        return@clickable
                    }
                    onSave(date, rating, content, photos)
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(rose)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("保存", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
