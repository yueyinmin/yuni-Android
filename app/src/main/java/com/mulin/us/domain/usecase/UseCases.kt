package com.mulin.us.domain.usecase

import com.mulin.us.domain.model.DiaryVisibility
import com.mulin.us.domain.repository.DiaryRepository
import com.mulin.us.domain.repository.MemoryRepository
import com.mulin.us.domain.repository.SyncRepository
import java.time.LocalDate

/**
 * UseCase 层（路线图 1.5 补齐）。
 *
 * 为什么现在加：
 * B0.1 时判断"标题空了填无标题""私密日记不上云"这些规则都写在 ViewModel 里，
 * 换页面就要抄一遍，抄漏一处就出事。抽到这里，规则只有一份。
 *
 * 边界：UseCase 只依赖 Domain 层的 Repository 接口，看不到 OkHttp / Room / CloudBase。
 * 调度后台同步这种"UI 副作用"留在 ViewModel，不塞进 UseCase。
 */

class CreateMemoryUseCase(private val memoryRepository: MemoryRepository) {
    suspend operator fun invoke(title: String, content: String, happenedAt: Long?): Long {
        require(title.isNotBlank() || content.isNotBlank()) { "标题和内容不能都为空" }
        return memoryRepository.add(
            title = title.ifBlank { "无标题" },
            content = content,
            happenedAt = happenedAt
        )
    }
}

class WriteDiaryUseCase(private val diaryRepository: DiaryRepository) {
    suspend operator fun invoke(
        ownerId: String,
        isPrivate: Boolean,
        content: String,
        entryEpochDay: Long = LocalDate.now().toEpochDay(),
        mood: String? = null,
        title: String? = null
    ): Long {
        require(content.isNotBlank()) { "日记内容不能为空" }
        require(ownerId.isNotBlank()) { "没有本机档案，先建档案" }
        return diaryRepository.add(
            ownerId = ownerId,
            visibility = if (isPrivate) DiaryVisibility.PRIVATE else DiaryVisibility.SHARED,
            title = title,
            content = content,
            entryEpochDay = entryEpochDay,
            mood = mood
        )
    }
}

class SyncNowUseCase(private val syncRepository: SyncRepository) {
    suspend operator fun invoke(reason: String = "manual"): Boolean {
        // 本地模式下 syncNow 是空操作，返回 false 让 UI 可以不显示"已同步"
        if (!syncRepository.cloudAvailable) return false
        syncRepository.syncNow(reason)
        return true
    }
}
