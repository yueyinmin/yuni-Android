package com.mulin.us.data.sync

import com.mulin.us.domain.model.SyncOperation
import com.mulin.us.domain.model.SyncStatus

/**
 * 冲突解决策略（B0 只定义规则，不做自动合并）。
 *
 * 工单要求"冲突不能静默覆盖"。所以这里的默认动作是 [Decision.AskUser]，
 * 而不是"谁最后写谁赢"——日记类内容一旦覆盖就是永久丢字。
 *
 * B4 会把 Decision 接到界面上（并排显示两个版本）；B0 只保证：
 * 遇到冲突时不覆盖、不丢数据、状态标成 CONFLICT。
 */
object ConflictResolver {

    enum class Decision {
        /** 直接用云端版本覆盖本地（只用于"本地没有未提交改动"的情况） */
        TakeCloud,

        /** 保留本地版本，稍后重新提交（用于"本地改了但云端版本没变"） */
        KeepLocal,

        /** 两边都变了：必须问用户，不自动决定 */
        AskUser
    }

    /**
     * 路线图 2.7：接入 serverVersion。
     * 旧的三参数调用保留默认值，新代码传本地/云端版本进来。
     * 云端版本 > 本地基线 = 这一轮云端也改过 → 双方并发修改，必须问用户。
     */
    fun decide(
        operation: SyncOperation,
        localHasPendingChanges: Boolean,
        remoteDeleted: Boolean,
        localServerVersion: Int = 0,
        remoteServerVersion: Int = 0
    ): Decision {
        val remoteChanged = remoteServerVersion > localServerVersion
        return when {
            // 对方删了、本地改了：不能自动丢本地的字
            remoteDeleted && localHasPendingChanges -> Decision.AskUser
            // 本地没改过 → 云端说什么就是什么（本地无字可丢，安全覆盖）
            !localHasPendingChanges -> Decision.TakeCloud
            // 本地改过 且 云端也改过 → 双方并发，必须问用户
            remoteChanged -> Decision.AskUser
            // 删除操作遇到冲突 → 交给用户确认，避免误删
            operation == SyncOperation.DELETE -> Decision.AskUser
            // 本地改过、云端没变 → 保留本地（KeepLocal 现在可达）
            else -> Decision.KeepLocal
        }
    }

    /** 冲突在本地要落到哪个状态：永远是 CONFLICT，绝不悄悄改成 SYNCED */
    fun statusFor(decision: Decision): SyncStatus = when (decision) {
        Decision.TakeCloud -> SyncStatus.SYNCED
        Decision.KeepLocal -> SyncStatus.PENDING_UPLOAD
        Decision.AskUser -> SyncStatus.CONFLICT
    }
}
