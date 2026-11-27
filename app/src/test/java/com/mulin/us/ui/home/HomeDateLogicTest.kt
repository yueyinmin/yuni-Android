package com.mulin.us.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class HomeDateLogicTest {

    private val today = LocalDate.of(2026, 11, 27)

    @Test
    fun `相恋天数使用同一个today基准`() {
        assertEquals(0L, homeDaysTogether(LocalDate.of(2026, 11, 27), today))
        assertEquals(1L, homeDaysTogether(LocalDate.of(2026, 11, 26), today))
        assertEquals(365L, homeDaysTogether(LocalDate.of(2025, 11, 27), today))
    }

    @Test
    fun `未来开始日按0处理`() {
        assertEquals(0L, homeDaysTogether(LocalDate.of(2026, 11, 28), today))
    }

    @Test
    fun `未设置开始日返回null`() {
        assertNull(homeDaysTogether(null, today))
    }
}
