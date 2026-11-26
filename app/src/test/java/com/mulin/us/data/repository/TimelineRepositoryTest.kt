package com.mulin.us.data.repository

import com.mulin.us.domain.model.Diary
import com.mulin.us.domain.model.DiaryVisibility
import com.mulin.us.domain.model.MeetingPlan
import com.mulin.us.domain.model.Memory
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
        wishes: List<Wish> = emptyList()
    ): TimelineRepositoryImpl = TimelineRepositoryImpl(
        memoryRepository = FakeMemory(memories),
        diaryRepository = FakeDiary(diaries),
        meetingRepository = FakeMeeting(meetings),
        wishRepository = FakeWish(wishes)
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
}
