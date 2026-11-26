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
import kotlinx.coroutines.flow.map
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

    // 这三个计数走 Repository 接口，不再直接 container.db.xxxDao()。
    // 原因：UI 层直接查 DAO 会绕过"私密日记只能看自己"这类规则的一致性检查点，
    // 而且换数据源时（比如以后加内存缓存）UI 得跟着改。见 B0.1 T03 依赖边界第 7 条。
    /** 完整时间线（工单 V3.1）：首页最近记录与回忆页共用同一来源 */
    val timeline: StateFlow<List<com.mulin.us.domain.model.TimelineEntry>> =
        container.timelineRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentTimeline: StateFlow<List<com.mulin.us.domain.model.TimelineEntry>> =
        container.timelineRepository.observeRecent(3)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val memoryCount: StateFlow<Int> = container.memoryRepository.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val diaryCount: StateFlow<Int> = container.diaryRepository.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val wishCount: StateFlow<Int> = container.wishRepository.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** 最近生成的邀请码：必须持久显示，不能只靠一闪而过的提示 */
    val lastInviteCode: MutableStateFlow<String?> = MutableStateFlow(null)

    /** 界面上"提示一行"用；不弹 Toast，保持 Compose 纯度 */
    val toast: MutableStateFlow<String?> = MutableStateFlow(null)

    val cloudStateText: String = container.cloudStateText()
    val cloudConfigured: Boolean = container.syncRepository.cloudAvailable

    /** 当前双人空间（B1 UI 用；未建空间时为 null） */
    val couple: StateFlow<com.mulin.us.domain.model.CoupleProfile?> =
        container.coupleRepository.couple
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun dismissToast() {
        toast.value = null
    }

    /** UI 主动提示（如复制邀请码成功） */
    fun showToast(message: String) {
        notify(message)
    }

    private fun notify(message: String) {
        toast.value = message
    }

    // ---------------- 动作 ----------------

    /** 相恋天数：从恋爱开始日算起，未设置返回 null */
    val daysTogether: StateFlow<Long?> = couple.map { c ->
        c?.relationshipStartEpochDay?.let { start ->
            (java.time.LocalDate.now().toEpochDay() - start).coerceAtLeast(0)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 工单 C3.3：用户主动确认后才写入 ISO 开始日 */
    fun confirmStartDate(iso: String) {
        viewModelScope.launch {
            container.coupleRepository.setStartDateIso(iso)
            notify("恋爱开始日已更新")
        }
    }

    fun setRelationshipStart(epochDay: Long) {
        viewModelScope.launch {
            container.coupleRepository.setRelationshipStart(epochDay)
            notify("恋爱开始日已保存")
        }
    }

    fun createLocalProfile(nickname: String) {
        viewModelScope.launch {
            val user = container.authRepository.createLocalProfile(nickname)
            notify("已创建本机档案：${user.nickname}")
        }
    }

    // ---------------- S5 互动 ----------------
    val todayQuestion: MutableStateFlow<com.mulin.us.domain.model.DailyQuestion?> = MutableStateFlow(null)
    val questionHistory: MutableStateFlow<List<com.mulin.us.domain.model.DailyQuestion>> = MutableStateFlow(emptyList())
    val messages: MutableStateFlow<List<com.mulin.us.domain.model.Message>> = MutableStateFlow(emptyList())
    val unreadCount: MutableStateFlow<Int> = MutableStateFlow(0)

    fun loadTodayQuestion() {
        val day = java.time.LocalDate.now().toEpochDay()
        viewModelScope.launch {
            container.questionRepository.today(day).fold(
                onSuccess = { todayQuestion.value = it },
                onFailure = { notify("加载问答失败：${it.message}") }
            )
        }
    }

    fun answerToday(answer: String) {
        val day = java.time.LocalDate.now().toEpochDay()
        viewModelScope.launch {
            container.questionRepository.answer(day, answer).fold(
                onSuccess = { revealed ->
                    notify(if (revealed) "双方都答了，答案已揭晓" else "已保存，等对方回答")
                    loadTodayQuestion()
                },
                onFailure = { notify(it.message ?: "提交失败") }
            )
        }
    }

    fun loadMessages() {
        viewModelScope.launch {
            container.messageRepository.list().fold(
                onSuccess = { messages.value = it },
                onFailure = { notify("加载消息失败：${it.message}") }
            )
            container.messageRepository.unreadCount().onSuccess { unreadCount.value = it }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) { notify("说点什么再发"); return }
        viewModelScope.launch {
            container.messageRepository.send(content.trim()).fold(
                onSuccess = { loadMessages() },
                onFailure = { notify(it.message ?: "发送失败") }
            )
        }
    }

    // 4.8/4.9 回忆照片：memoryCloudId -> 本地路径列表
    /** 照片 Flow 缓存（按 Room 的 memoryLocalId）：数据来自 memory_photo 表，重启仍在 */
    private val photoFlows = mutableMapOf<Long, StateFlow<List<com.mulin.us.domain.model.MemoryPhoto>>>()

    fun photosFor(memoryLocalId: Long): StateFlow<List<com.mulin.us.domain.model.MemoryPhoto>> =
        photoFlows.getOrPut(memoryLocalId) {
            container.memoryPhotoRepository.observeByMemory(memoryLocalId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    fun addPhoto(memoryLocalId: Long, memoryCloudId: String, uri: android.net.Uri) {
        viewModelScope.launch {
            container.memoryPhotoRepository.add(memoryLocalId, memoryCloudId, uri).fold(
                onSuccess = { photo ->
                    notify(if (photo.synced) "照片已存本地并上传云端" else "照片已存本地（云端暂不可用）")
                },
                onFailure = { notify(it.message ?: "加图失败") }
            )
        }
    }

    // S6 未来信
    val letters: MutableStateFlow<List<com.mulin.us.domain.model.Letter>> = MutableStateFlow(emptyList())
    val openedLetter: MutableStateFlow<String?> = MutableStateFlow(null)

    fun loadLetters() {
        viewModelScope.launch {
            container.letterRepository.list().fold(
                onSuccess = { letters.value = it },
                onFailure = { notify("加载信件失败：${it.message}") }
            )
        }
    }

    fun createLetter(title: String, content: String, unlockAt: Long) {
        viewModelScope.launch {
            container.letterRepository.create(title, content, unlockAt).fold(
                onSuccess = { notify("信已封存，到时间才能打开"); loadLetters() },
                onFailure = { notify(it.message ?: "创建失败") }
            )
        }
    }

    fun openLetter(id: String) {
        viewModelScope.launch {
            container.letterRepository.open(id).fold(
                onSuccess = { openedLetter.value = it },
                onFailure = { notify(it.message ?: "打不开") }
            )
        }
    }

    // 5.6 回忆评论
    val comments: MutableStateFlow<List<com.mulin.us.domain.model.Comment>> = MutableStateFlow(emptyList())
    var activeCommentMemory: String? = null

    fun loadComments(memoryCloudId: String) {
        activeCommentMemory = memoryCloudId
        viewModelScope.launch {
            container.commentRepository.list(memoryCloudId).fold(
                onSuccess = { comments.value = it },
                onFailure = { notify("加载评论失败：${it.message}") }
            )
        }
    }

    fun addComment(content: String) {
        val mid = activeCommentMemory ?: return
        if (content.isBlank()) return
        viewModelScope.launch {
            container.commentRepository.add(mid, content).fold(
                onSuccess = { loadComments(mid) },
                onFailure = { notify(it.message ?: "评论失败") }
            )
        }
    }

    fun setDailyReminder(hour: Int, minute: Int) {
        container.reminderScheduler.scheduleDaily(hour, minute)
        notify("已设每天 ${hour}:${minute.toString().padStart(2, '0')} 本地提醒")
    }

    fun cancelDailyReminder() {
        container.reminderScheduler.cancel()
        notify("已关闭本地提醒")
    }

    fun markMessageRead(id: String) {
        viewModelScope.launch {
            container.messageRepository.markRead(id).onSuccess { loadMessages() }
        }
    }

    // ---------------- B1 账号与空间 ----------------

    fun register(username: String, password: String) {
        viewModelScope.launch {
            val r = container.authRepository.register(username.trim(), password)
            notify(r.fold(
                onSuccess = { "注册成功：${it.nickname}" },
                onFailure = { "注册失败：${it.message ?: "未知错误"}" }
            ))
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            val r = container.authRepository.login(username.trim(), password)
            notify(r.fold(
                onSuccess = { "登录成功：${it.nickname}" },
                onFailure = { "登录失败：${it.message ?: "未知错误"}" }
            ))
        }
    }

    fun logout() {
        viewModelScope.launch {
            container.authRepository.logout()
            notify("已退出云端账号（本地数据保留）")
        }
    }

    /** A 一键建空间+发邀请码：拿到 coupleId 才能发码，合并成一步 */
    fun createSpaceAndInvite(relationshipStartEpochDay: Long?) {
        viewModelScope.launch {
            val c = container.coupleRepository.createCloudCouple(relationshipStartEpochDay)
            if (c.isFailure) {
                notify("建空间失败：${c.exceptionOrNull()?.message ?: "请先登录"}")
                return@launch
            }
            val inv = container.coupleRepository.createInvite()
            inv.onSuccess { lastInviteCode.value = it }
            notify(inv.fold(
                onSuccess = { "空间已建好，邀请码：$it（显示在下方，发给另一半）" },
                onFailure = { "空间已建，但发码失败：${it.message ?: "未知错误"}" }
            ))
        }
    }

    fun createInvite() {
        viewModelScope.launch {
            val r = container.coupleRepository.createInvite()
            r.onSuccess { lastInviteCode.value = it }
            notify(r.fold(
                onSuccess = { "邀请码：$it（显示在下方，发给你另一半）" },
                onFailure = { "生成失败：${it.message ?: "请先登录并建空间"}" }
            ))
        }
    }

    fun acceptInvite(code: String) {
        if (code.isBlank()) {
            notify("请输入邀请码")
            return
        }
        viewModelScope.launch {
            val r = container.coupleRepository.acceptInvite(code.trim().uppercase())
            notify(r.fold(
                onSuccess = { "加入成功，双人空间已绑定" },
                onFailure = { "加入失败：${it.message ?: "邀请码无效或已用"}" }
            ))
        }
    }

    fun addMemory(title: String, content: String) {
        viewModelScope.launch {
            runCatching {
                container.createMemoryUseCase(title, content, System.currentTimeMillis())
            }.onSuccess {
                notify("回忆已保存到本地")
                if (cloudConfigured) container.syncScheduler.requestImmediate("after-save")
            }.onFailure { notify(it.message ?: "保存失败") }
        }
    }

    fun addDiary(private: Boolean, content: String) {
        val ownerId = currentUser.value?.id ?: "local-me"
        viewModelScope.launch {
            runCatching {
                container.writeDiaryUseCase(ownerId, private, content)
            }.onSuccess {
                notify(if (private) "私密日记已保存（仅本机，不上云）" else "共同日记已保存")
            }.onFailure { notify(it.message ?: "保存失败") }
        }
    }

    /** 轻记日记页：写一篇日记（带日期/星级/照片），mood 存 "3★" 形式 */
    fun writeDiaryWithMood(
        date: java.time.LocalDate,
        rating: Int,
        content: String,
        photos: List<android.net.Uri>
    ) {
        val ownerId = currentUser.value?.id ?: "local-me"
        viewModelScope.launch {
            runCatching {
                container.writeDiaryUseCase(
                    ownerId = ownerId,
                    isPrivate = false,
                    content = content,
                    entryEpochDay = date.toEpochDay(),
                    mood = "$rating★"
                )
            }.onSuccess {
                saveDiaryPhotos(it, photos)
                notify("日记已保存")
            }.onFailure { notify(it.message ?: "保存失败") }
        }
    }

    /** 轻记日记页：写一篇回忆（带日期/星级/照片） */
    fun writeMemoryWithMood(
        date: java.time.LocalDate,
        rating: Int,
        content: String,
        photos: List<android.net.Uri>
    ) {
        viewModelScope.launch {
            runCatching {
                container.memoryRepository.add(
                    title = "$rating★",
                    content = content,
                    happenedAt = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    mood = "$rating★"
                )
            }.onSuccess { id ->
                saveMemoryPhotos(id, photos)
                notify("回忆已保存")
                if (cloudConfigured) container.syncScheduler.requestImmediate("memory-added")
            }.onFailure { notify(it.message ?: "保存失败") }
        }
    }

    /** 照片先压缩存本地私有目录（Lightnote 同款：系统 Uri 会失效），有云端再传 */
    private suspend fun saveDiaryPhotos(diaryLocalId: Long, photos: List<android.net.Uri>) {
        // 日记照片暂时复用 memory_photo（以 diaryLocalId 为外键语义占位），
        // 云端没有 diary 图片接口时仍保证本地可看：直接压缩存私有目录。
        photos.forEachIndexed { index, uri ->
            val bytes = container.imageStore.compress(uri) ?: return@forEachIndexed
            container.imageStore.saveLocal(bytes, "diary_${diaryLocalId}_${index}.jpg")
        }
    }

    private suspend fun saveMemoryPhotos(memoryLocalId: Long, photos: List<android.net.Uri>) {
        // 新建回忆尚未同步，cloudId 为空 → 先压缩存本地私有目录；
        // 用户可在回忆列表用「加图」按钮重新走上传（复用已有通道）。
        photos.forEachIndexed { index, uri ->
            val bytes = container.imageStore.compress(uri) ?: return@forEachIndexed
            container.imageStore.saveLocal(bytes, "memory_${memoryLocalId}_${index}.jpg")
        }
    }

    fun deleteDiary(localId: Long) {
        viewModelScope.launch {
            container.diaryRepository.softDelete(localId)
            notify("日记已删除")
        }
    }

    fun deleteMemory(localId: Long) {
        viewModelScope.launch {
            container.memoryRepository.softDelete(localId)
            notify("回忆已删除")
        }
    }

    fun addWish(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            container.wishRepository.add(title = title, note = null, targetAt = null)
            notify("愿望已加入清单")
            if (cloudConfigured) container.syncScheduler.requestImmediate("wish-added")
        }
    }

    fun toggleWishDone(localId: Long, done: Boolean) {
        viewModelScope.launch {
            container.wishRepository.markDone(localId, done)
            notify(if (done) "愿望已完成" else "愿望已恢复")
            if (cloudConfigured) container.syncScheduler.requestImmediate("wish-done")
        }
    }

    fun deleteWish(localId: Long) {
        viewModelScope.launch {
            container.wishRepository.softDelete(localId)
            notify("愿望已删除")
            if (cloudConfigured) container.syncScheduler.requestImmediate("wish-deleted")
        }
    }

    // 6.5/6.6 见面计划
    val meetings: StateFlow<List<com.mulin.us.domain.model.MeetingPlan>> =
        container.meetingPlanRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addMeeting(plannedAt: Long, place: String?, note: String?) {
        viewModelScope.launch {
            container.meetingPlanRepository.add(plannedAt, place, note)
            notify("见面计划已保存")
            if (cloudConfigured) container.syncScheduler.requestImmediate("meeting-added")
        }
    }

    fun deleteMeeting(localId: Long) {
        viewModelScope.launch {
            container.meetingPlanRepository.softDelete(localId)
            notify("见面计划已删除")
            if (cloudConfigured) container.syncScheduler.requestImmediate("meeting-deleted")
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            when (container.syncNowUseCase("manual")) {
                true -> Unit
                false -> notify("本地模式，无需同步")
            }
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
