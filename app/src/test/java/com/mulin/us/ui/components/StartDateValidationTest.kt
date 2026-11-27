package com.mulin.us.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StartDateValidationTest {

    private val today = LocalDate.of(2026, 11, 27)

    @Test
    fun `历史日期和今天都有效`() {
        assertTrue(validStartDate(LocalDate.of(2020, 2, 29), today))
        assertTrue(validStartDate(today, today))
    }

    @Test
    fun `未来日期无效`() {
        assertFalse(validStartDate(today.plusDays(1), today))
        assertFalse(validStartDate(today.plusYears(1), today))
    }

    @Test
    fun `不存在日期与非闰年229无效`() {
        assertFalse(validStartDate(runCatching { LocalDate.of(2025, 2, 29) }.getOrNull(), today))
        assertFalse(validStartDate(null, today))
    }
}
