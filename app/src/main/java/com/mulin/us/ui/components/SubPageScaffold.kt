package com.mulin.us.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable

/**
 * 二级页面外壳：顶部标题 + 返回。
 *
 * 为什么统一成一个：二级页（日记、愿望）是"从 Tab 里进去又退回来"的页面，
 * 返回按钮的位置、大小、文案必须完全一致，否则用户每进一个页面都要重新找返回在哪。
 *
 * 注意：二级页**不挂底部导航**（工单 7.3 第 5 条：不应出现底部导航重复嵌套）。
 * 底栏只属于五个顶级 Tab。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubPageScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    // 不引入 material-icons 依赖（省 3MB），用文字箭头
                    TextButton(onClick = onBack) { Text("← 返回") }
                }
            )
        }
    ) { padding ->
        content(padding)
    }
}
