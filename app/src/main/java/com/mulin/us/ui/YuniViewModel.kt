package com.mulin.us.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mulin.us.core.AppContainer
import com.mulin.us.data.sync.SyncStateStore
import com.mulin.us.domain.model.Diary
import com.mulin.us.domain.model.DiaryVisibility
import com.mulin.us.domain.model.LocalUser
import com.mulin.us.domain.model.Memory
import com.mulin.us.domain.model.Wish
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * B0 阶段共用的 ViewModel。
 *
 * 为什么只有一个：
 * B0 只有骨架页，拆成 5 个 VM 会产生大量重复的样板；
 * B1 开始按页面拆分（HomeViewModel / MemoryViewModel / ...），这里的 action 会原样搬过去。
 *
 * 注意它只依赖 Domain 层的 Repository 接口，
 * 既看不到 OkHttp，也看不到 CloudBase —— 这是"云端可替换"的关键一条。
 */
class YuniViewModel(private val container: AppContainer) : ViewModel() {

    val currentUser: StateFlow<LocalUser?> = container.authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val memories: StateFlow<List<Memory>> = container.memoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val diaries: StateFlow<List<Diary>> = container.diaryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val wishes: StateFlow<List<Wish>> = container.wishRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pendingCount: StateFlow<Int> = container.syncRepository.observePendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val lastSyncAt: StateFlow<Long> = container.syncRepository.observeLastSyncAt()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val lastSyncResult: StateFlow<String> = container.syncRepository.observeLastResult()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "尚未同步")

    val memoryCount: StateFlow<Int> = container.db.memoryDao().observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val diaryCount: StateFlow<Int> = container.db.diaryDao().observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val wishCount: StateFlow<Int> = container.db.wishDao().observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** 界面上"提示一行"用；不弹 Toast，保持 Compose 纯度 */
    val toast: MutableStateFlow<String?> = MutableStateFlow(null)

    val cloudStateText: String = container.cloudStateText()
    val cloudConfigured: Boolean = container.syncRepository.cloudAvailable

    fun dismissToast() {
        toast.value = null
    }

    private fun notify(message: String) {
        toast.value = message
    }

    // ---------------- 动作 ----------------

    fun createLocalProfile(nickname: String) {
        viewModelScope.launch {
            val user = container.authRepository.createLocalProfile(nickname)
            notify("已创建本机档案：${user.nickname}")
        }
    }

    fun addMemory(title: String, content: String) {
        if (title.isBlank() && content.isBlank()) {
            notify("标题和内容不能都为空")
            return
        }
        viewModelScope.launch {
            container.memoryRepository.add(
                title = title.ifBlank { "无标题" },
                content = content,
                happenedAt = System.currentTimeMillis()
            )
            notify("回忆已保存到本地")
            if (cloudConfigured) container.syncScheduler.requestImmediate("after-save")
        }
    }

    fun addDiary(private: Boolean, content: String) {
        if (content.isBlank()) {
            notify("日记内容不能为空")
            return
        }
        val ownerId = currentUser.value?.id ?: "local-me"
        viewModelScope.launch {
            container.diaryRepository.add(
                ownerId = ownerId,
                visibility = if (private) DiaryVisibility.PRIVATE else DiaryVisibility.SHARED,
                title = null,
                content = content,
                entryEpochDay = LocalDate.now().toEpochDay()
            )
            notify(if (private) "私密日记已保存（仅本机，不上云）" else "共同日记已保存")
        }
    }

    fun addWish(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            container.wishRepository.add(title = title, note = null, targetAt = null)
            notify("愿望已加入清单")
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            container.syncCoordinator.runOnce(reason = "manual")
        }
    }

    fun scheduleBackgroundSync() {
        container.syncScheduler.requestImmediate("manual-enqueue")
        notify("已排入后台同步队列")
    }

    fun resetSyncStateText() {
        SyncStateStore.lastResult.value = "尚未同步"
    }
}

class YuniViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        YuniViewModel(container) as T
}
