package com.mulin.us.data.repository

import com.mulin.us.domain.model.CoupleProfile
import com.mulin.us.domain.model.DailyQuestion
import com.mulin.us.domain.model.Diary
import com.mulin.us.domain.model.DiaryVisibility
import com.mulin.us.domain.model.Letter
import com.mulin.us.domain.model.LetterStatus
import com.mulin.us.domain.model.MeetingPlan
import com.mulin.us.domain.model.Memory
import com.mulin.us.domain.model.MemoryPhoto
import com.mulin.us.domain.model.SyncStatus
import com.mulin.us.domain.model.TimelineFilter
import com.mulin.us.domain.model.TimelineSource
import com.mulin.us.domain.model.Wish
import com.mulin.us.data.repository.impl.TimelineRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * 阶段 B 时间线聚合单测（工单 F 节 7~11）：
 * 权限排除、eventAt 排序、分页、筛选、补录历史。
 */
class TimelineRepositoryTest {

    private fun repo(
        memories: List<Memory> = emptyList(),
        diaries: List<Diary> = emptyList(),
        meetings: List<MeetingPlan> = emptyList(),
        wishes: List<Wish> = emptyList(),
        questions: List<DailyQuestion> = emptyList(),
        letters: List<Letter> = emptyList(),
        couple: CoupleProfile? = null,
        photos: List<MemoryPhoto> = emptyList()
    ): TimelineRepositoryImpl = TimelineRepositoryImpl(
        memoryRepository = FakeMemory(memories),
        diaryRepository = FakeDiary(diaries),
        meetingRepository = FakeMeeting(meetings),
        wishRepository = FakeWish(wishes),
        // Fake 模拟 SQL 层过滤：未揭晓/未解锁根本不会从 Flow 发出来
        questionRepository = FakeQuestion(questions.filter { it.revealed }),
        letterRepository = FakeLetter(letters.filter { it.status == LetterStatus.UNLOCKED }),
        coupleRepository = FakeCouple(couple),
        memoryPhotoRepository = FakePhoto(photos)
    )

    private fun memory(id: Long, day: LocalDate, deleted: Boolean = false) = Memory(
        localId = id, title = "回忆$id", content = "内容$id",
        happenedAt = day.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
        createdAt = System.currentTimeMillis(), deletedAt = if (deleted) 1L else null
    )

    private fun diary(id: Long, day: LocalDate, private: Boolean) = Diary(
        localId = id, ownerId = "u1",
        visibility = if (private) DiaryVisibility.PRIVATE else DiaryVisibility.SHARED,
        content = "日记$id", entryEpochDay = day.toEpochDay()
    )

    @Test
    fun `私密日记完全排除`() = runBlocking {
        val r = repo(
            diaries = listOf(diary(1, LocalDate.of(2026, 11, 20), false),
                diary(2, LocalDate.of(2026, 11, 21), true))
        )
        val list = r.observeAll().first()
        assertEquals(1, list.size)
        assertEquals(TimelineSource.DIARY, list[0].sourceType)
        assertEquals(1L, list[0].sourceLocalId)
    }

    @Test
    fun `软删除记录排除`() = runBlocking {
        val r = repo(memories = listOf(
            memory(1, LocalDate.of(2026, 11, 1)),
            memory(2, LocalDate.of(2026, 11, 2), deleted = true)
        ))
        assertEquals(1, r.observeAll().first().size)
    }

    @Test
    fun `未完成愿望排除`() = runBlocking {
        val done = Wish(localId = 1, title = "已完成", doneAt = 1000L)
        val todo = Wish(localId = 2, title = "未完成", doneAt = null)
        val r = repo(wishes = listOf(done, todo))
        val list = r.observeAll().first()
        assertEquals(1, list.size)
        assertEquals(TimelineSource.WISH, list[0].sourceType)
        assertEquals("已完成", list[0].title)
    }

    @Test
    fun `按eventAt倒序而非createdAt`() = runBlocking {
        // 补录的历史回忆 createdAt 是现在，但 eventAt 很早 → 应排在后面
        val old = Memory(localId = 1, title = "补录", content = "c",
            happenedAt = LocalDate.of(2020, 1, 1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            createdAt = 999999L)
        val recent = Memory(localId = 2, title = "今天", content = "c",
            happenedAt = LocalDate.of(2026, 11, 26).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            createdAt = 1L)
        val r = repo(memories = listOf(old, recent))
        val list = r.observeAll().first()
        assertEquals(2L, list[0].sourceLocalId)
        assertEquals(1L, list[1].sourceLocalId)
    }

    @Test
    fun `分页无重复无遗漏`() = runBlocking {
        val days = (1..7).map { LocalDate.of(2026, 11, it) }
        val r = repo(memories = days.mapIndexed { i, d -> memory(i.toLong() + 1, d) })
        val p1 = r.observePage(0, 3).first().map { it.sourceLocalId }
        val p2 = r.observePage(3, 3).first().map { it.sourceLocalId }
        val p3 = r.observePage(6, 3).first().map { it.sourceLocalId }
        assertEquals(listOf(7L, 6L, 5L), p1)
        assertEquals(listOf(4L, 3L, 2L), p2)
        assertEquals(listOf(1L), p3)
        assertEquals(7, (p1 + p2 + p3).distinct().size)
    }

    @Test
    fun `年份月份与类型筛选`() = runBlocking {
        val r = repo(
            memories = listOf(memory(1, LocalDate.of(2026, 11, 5))),
            diaries = listOf(diary(2, LocalDate.of(2026, 10, 6), false)),
            meetings = listOf(MeetingPlan(localId = 3, plannedAt = LocalDate.of(2025, 11, 7)
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()))
        )
        assertEquals(2, r.observeFiltered(year = 2026).first().size)
        assertEquals(1, r.observeFiltered(year = 2026, month = 11).first().size)
        assertEquals(1, r.observeFiltered(filter = TimelineFilter.MEETING).first().size)
        assertEquals(1, r.observeFiltered(filter = TimelineFilter.DIARY).first().size)
    }

    @Test
    fun `悄悄话不作为源接入`() = runBlocking {
        // TimelineRepositoryImpl 构造函数根本没有 MessageRepository 参数 —— 结构上保证排除
        val ctorParams = TimelineRepositoryImpl::class.java.constructors.first().parameterTypes
        assertFalse(ctorParams.any { it.simpleName.contains("Message") })
    }

    @Test
    fun `最近记录是完整时间线的前N条`() = runBlocking {
        val r = repo(memories = (1..5).map { memory(it.toLong(), LocalDate.of(2026, 11, it)) })
        val all = r.observeAll().first()
        val recent = r.observeRecent(3).first()
        assertEquals(all.take(3), recent)
        assertTrue(recent.size == 3)
    }

    @Test
    fun `已揭晓问答接入且未揭晓被排除`() = runBlocking {
        val day = LocalDate.of(2026, 11, 20).toEpochDay()
        val revealed = DailyQuestion(
            localId = 1, questionEpochDay = day, content = "最难忘的一次旅行",
            myAnswer = "海边", partnerAnswer = "山顶", revealed = true
        )
        val hidden = DailyQuestion(
            localId = 2, questionEpochDay = day + 1, content = "还没揭晓",
            myAnswer = "我答了", partnerAnswer = null, revealed = false
        )
        val r = repo(questions = listOf(revealed, hidden))
        val list = r.observeAll().first()
        assertEquals(1, list.size)
        assertEquals(TimelineSource.QUESTION, list[0].sourceType)
        assertTrue(list[0].summary.contains("TA：山顶"))
    }

    @Test
    fun `未解锁未来信排除且摘要不泄露正文`() = runBlocking {
        val unlocked = Letter(
            localId = 1, cloudId = "l1", title = "给一年后的我们",
            unlockAt = LocalDate.of(2026, 11, 21).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            status = LetterStatus.UNLOCKED, senderId = "me", receiverId = "ta", createdAt = 1000L
        )
        val locked = unlocked.copy(localId = 2, cloudId = "l2", status = LetterStatus.LOCKED)
        val r = repo(letters = listOf(unlocked, locked))
        val list = r.observeAll().first()
        assertEquals(1, list.size)
        assertEquals(TimelineSource.LETTER, list[0].sourceType)
        assertFalse(list[0].summary.contains("正文"))
    }

    @Test
    fun `纪念日生成独立条目并可筛选`() = runBlocking {
        val start = LocalDate.now().minusYears(1)
        val r = repo(couple = CoupleProfile(id = "c1", memberAId = "a", startDateIso = start.toString()))
        val all = r.observeAll().first()
        val anniversaries = r.observeFiltered(filter = TimelineFilter.ANNIVERSARY).first()
        assertTrue(anniversaries.isNotEmpty())
        assertTrue(anniversaries.all { it.sourceType == TimelineSource.ANNIVERSARY })
        assertTrue(all.any { it.sourceType == TimelineSource.ANNIVERSARY })
    }

    @Test
    fun `照片路径填充到对应回忆条目`() = runBlocking {
        val file = File.createTempFile("yuni-timeline-", ".jpg")
        val r = repo(
            memories = listOf(memory(1, LocalDate.of(2026, 11, 10)), memory(2, LocalDate.of(2026, 11, 11))),
            photos = listOf(MemoryPhoto(memoryLocalId = 1, memoryCloudId = "m1", localPath = file.absolutePath))
        )
        val list = r.observeAll().first()
        val withPhoto = list.first { it.sourceLocalId == 1L }
        val without = list.first { it.sourceLocalId == 2L }
        assertEquals(file.absolutePath, withPhoto.imagePath)
        assertEquals(null, without.imagePath)
        file.delete()
        Unit
    }

    // ---- Fakes ----
    private class FakeMemory(private val data: List<Memory>) : com.mulin.us.domain.repository.MemoryRepository {
        override fun observeAll(): Flow<List<Memory>> = flowOf(data)
        override fun observeCount(): Flow<Int> = flowOf(data.size)
        override suspend fun add(title: String, content: String, happenedAt: Long?, mood: String?) = 0L
        override suspend fun update(model: Memory) = Unit
        override suspend fun softDelete(localId: Long) = Unit
    }

    private class FakeDiary(private val data: List<Diary>) : com.mulin.us.domain.repository.DiaryRepository {
        override fun observeAll(): Flow<List<Diary>> = flowOf(data)
        override fun observeByVisibility(v: DiaryVisibility): Flow<List<Diary>> =
            flowOf(data.filter { it.visibility == v })
        override fun observeCount(): Flow<Int> = flowOf(data.size)
        override suspend fun add(ownerId: String, visibility: DiaryVisibility, title: String?,
                                 content: String, entryEpochDay: Long, mood: String?) = 0L
        override suspend fun update(model: Diary) = Unit
        override suspend fun softDelete(localId: Long) = Unit
    }

    private class FakeMeeting(private val data: List<MeetingPlan>) : com.mulin.us.domain.repository.MeetingPlanRepository {
        override fun observeAll(): Flow<List<MeetingPlan>> = flowOf(data)
        override suspend fun add(plannedAt: Long, place: String?, note: String?) = 0L
        override suspend fun softDelete(localId: Long) = Unit
    }

    private class FakeWish(private val data: List<Wish>) : com.mulin.us.domain.repository.WishRepository {
        override fun observeAll(): Flow<List<Wish>> = flowOf(data)
        override fun observeCount(): Flow<Int> = flowOf(data.size)
        override suspend fun add(title: String, note: String?, targetAt: Long?) = 0L
        override suspend fun markDone(localId: Long, done: Boolean) = Unit
        override suspend fun softDelete(localId: Long) = Unit
    }

    private class FakeQuestion(private val data: List<DailyQuestion>) : com.mulin.us.domain.repository.QuestionRepository {
        override fun observeRevealed(): Flow<List<DailyQuestion>> = flowOf(data.filter { it.revealed })
        override suspend fun today(epochDay: Long): Result<DailyQuestion> =
            data.firstOrNull { it.questionEpochDay == epochDay }?.let { Result.success(it) }
                ?: Result.failure(NoSuchElementException())
        override suspend fun answer(epochDay: Long, answer: String): Result<Boolean> = Result.success(true)
        override suspend fun history(limit: Int): Result<List<DailyQuestion>> = Result.success(data)
    }

    private class FakeLetter(private val data: List<Letter>) : com.mulin.us.domain.repository.LetterRepository {
        override fun observeUnlocked(): Flow<List<Letter>> = flowOf(data.filter { it.status == LetterStatus.UNLOCKED })
        override suspend fun create(title: String, content: String, unlockAt: Long, kind: String): Result<Letter> =
            Result.success(data.firstOrNull() ?: Letter(title = title, unlockAt = unlockAt, senderId = "", receiverId = ""))
        override suspend fun list(): Result<List<Letter>> = Result.success(data)
        override suspend fun open(id: String): Result<String> = Result.success("正文")
    }

    private class FakeCouple(private val value: CoupleProfile?) : com.mulin.us.domain.repository.CoupleRepository {
        override val couple: Flow<CoupleProfile?> = flowOf(value)
        override suspend fun createLocalCouple(relationshipStartEpochDay: Long?): CoupleProfile =
            value ?: CoupleProfile(id = "local", memberAId = "me")
        override suspend fun createCloudCouple(relationshipStartEpochDay: Long?): Result<CoupleProfile> =
            Result.success(createLocalCouple(relationshipStartEpochDay))
        override suspend fun createInvite(): Result<String> = Result.success("CODE")
        override suspend fun acceptInvite(code: String): Result<Unit> = Result.success(Unit)
        override suspend fun setRelationshipStart(epochDay: Long) = Unit
        override suspend fun setStartDateIso(iso: String): Result<Unit> = Result.success(Unit)
        override suspend fun refreshStartDateFromRemote(): Result<Unit> = Result.success(Unit)
        override suspend fun dissolve(): Result<Unit> = Result.success(Unit)
    }

    private class FakePhoto(private val data: List<MemoryPhoto>) : com.mulin.us.domain.repository.MemoryPhotoRepository {
        override fun observeByMemory(memoryLocalId: Long): Flow<List<MemoryPhoto>> =
            flowOf(data.filter { it.memoryLocalId == memoryLocalId })
        override fun observeAll(): Flow<List<MemoryPhoto>> = flowOf(data)
        override suspend fun add(memoryLocalId: Long, memoryCloudId: String, uri: android.net.Uri): Result<MemoryPhoto> =
            Result.success(data.firstOrNull() ?: MemoryPhoto(memoryLocalId = memoryLocalId,
                memoryCloudId = memoryCloudId, localPath = ""))
    }
}
