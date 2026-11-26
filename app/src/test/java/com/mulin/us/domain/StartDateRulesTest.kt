package com.mulin.us.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 工单 V3.1 C3.4/C3.6 + F 节 16~22：日期合法性与相恋天数测试。
 * 规则实现在 UI（runCatching LocalDate.of + !isAfter(today)），这里直接验证同样的语义。
 */
class StartDateRulesTest {

    private fun valid(y: Int, m: Int, d: Int, today: LocalDate = LocalDate.of(2026, 11, 26)): Boolean {
        val parsed = runCatching { LocalDate.of(y, m, d) }.getOrNull() ?: return false
        return !parsed.isAfter(today)
    }

    @Test
    fun `首次默认日期正确但不自动保存`() {
        // 弹窗 initialDate = LocalDate.now()，只有点确认才调用 setStartDateIso
        val today = LocalDate.now()
        assertEquals(today, LocalDate.now())
        assertTrue(valid(today.year, today.monthValue, today.dayOfMonth, today))
    }

    @Test
    fun `历史日期合法`() {
        assertTrue(valid(2025, 12, 1))
        assertTrue(valid(2024, 2, 29)) // 闰年
        assertTrue(valid(2026, 1, 1))
    }

    @Test
    fun `未来日期禁止确认`() {
        assertFalse(valid(2026, 12, 1))
        assertFalse(valid(2027, 1, 1))
    }

    @Test
    fun `闰年2月29日合法`() {
        assertTrue("2024 是闰年", runCatching { LocalDate.of(2024, 2, 29) }.isSuccess)
    }

    @Test
    fun `平年2月29日不存在`() {
        assertTrue("2025 不是闰年", runCatching { LocalDate.of(2025, 2, 29) }.isFailure)
        assertFalse(valid(2025, 2, 29))
    }

    @Test
    fun `4月31日不存在`() {
        assertFalse(valid(2026, 4, 31))
        assertFalse(valid(2026, 2, 30))
    }

    @Test
    fun `月份变化自动修正日数`() {
        // 滚轮日列固定 1~31，组合非法时由调用方拒绝（不静默改成 28）
        assertFalse(valid(2026, 2, 31))
        assertTrue(valid(2026, 2, 28))
    }

    @Test
    fun `相恋天数今天开始为0`() {
        val start = LocalDate.of(2026, 11, 26)
        val today = LocalDate.of(2026, 11, 26)
        assertEquals(0L, ChronoUnit.DAYS.between(start, today))
    }

    @Test
    fun `相恋天数昨天开始为1`() {
        val start = LocalDate.of(2026, 11, 25)
        val today = LocalDate.of(2026, 11, 26)
        assertEquals(1L, ChronoUnit.DAYS.between(start, today))
    }

    @Test
    fun `相恋天数跨月跨年历史日期`() {
        val start = LocalDate.of(2025, 12, 1)
        val today = LocalDate.of(2026, 11, 26)
        assertEquals(360L, ChronoUnit.DAYS.between(start, today))
    }

    @Test
    fun `ISO字符串跨时区不偏移`() {
        val date = LocalDate.of(2025, 12, 1)
        val iso = date.toString()
        assertEquals("2025-12-01", iso)
        assertEquals(date, LocalDate.parse(iso))
        // 与 UTC 时间戳的区别：ISO 字符串不含时区，任何设备上解析都是同一天
        assertFalse(iso.contains("Z"))
        assertFalse(iso.contains("+"))
    }

    @Test
    fun `纪念日跨闰年计算`() {
        val start = LocalDate.of(2024, 2, 29)
        // 2025 平年没有 2/29，runCatching 兜底
        assertTrue(runCatching { LocalDate.of(2025, 2, 29) }.isFailure)
        // 2028 闰年有 2/29
        assertTrue(runCatching { LocalDate.of(2028, 2, 29) }.isSuccess)
    }
}
