package com.mulin.us.data.sync

import com.mulin.us.domain.model.SyncOperation
import com.mulin.us.domain.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 冲突策略的单元测试。
 *
 * 为什么先测这个：
 * 同步里最容易写错、后果最重的就是"冲突时到底做了什么"。
 * 一个 if 写反，用户三个月的日记就没了。
 *
 * 所以四条不变量必须被钉住：
 * 1. 本地没改动 → 可以接受云端版本；
 * 2. 本地改了、云端也改了 → 必须问用户，不许自动选边；
 * 3. 对方删除 + 本地修改 → 必须问用户（不能悄悄删掉本地的字）；
 * 4. 任何 AskUser 的结论，落到本地状态都只能是 CONFLICT。
 */
class ConflictResolverTest {

    @Test
    fun `本地没有改动时采用云端版本`() {
        val decision = ConflictResolver.decide(
            operation = SyncOperation.UPDATE,
            localHasPendingChanges = false,
            remoteDeleted = false
        )
        assertEquals(ConflictResolver.Decision.TakeCloud, decision)
    }

    @Test
    fun `双方都改了必须问用户`() {
        val decision = ConflictResolver.decide(
            operation = SyncOperation.UPDATE,
            localHasPendingChanges = true,
            remoteDeleted = false
        )
        assertEquals(ConflictResolver.Decision.AskUser, decision)
    }

    @Test
    fun `对方删除而本地改过 不能静默覆盖`() {
        val decision = ConflictResolver.decide(
            operation = SyncOperation.UPDATE,
            localHasPendingChanges = true,
            remoteDeleted = true
        )
        assertEquals(ConflictResolver.Decision.AskUser, decision)
    }

    @Test
    fun `删除操作遇到冲突一律问用户`() {
        val decision = ConflictResolver.decide(
            operation = SyncOperation.DELETE,
            localHasPendingChanges = true,
            remoteDeleted = false
        )
        assertEquals(ConflictResolver.Decision.AskUser, decision)
    }

    @Test
    fun `问用户的分支状态必须是 CONFLICT`() {
        assertEquals(
            SyncStatus.CONFLICT,
            ConflictResolver.statusFor(ConflictResolver.Decision.AskUser)
        )
    }
}
