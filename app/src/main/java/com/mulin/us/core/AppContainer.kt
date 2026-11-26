package com.mulin.us.core

import android.content.Context
import com.mulin.us.data.auth.TokenVault
import com.mulin.us.data.local.YuniDatabase
import com.mulin.us.data.remote.AuthenticationDataSource
import com.mulin.us.data.remote.CoupleRemoteDataSource
import com.mulin.us.data.remote.MemoryRemoteDataSource
import com.mulin.us.data.remote.StorageDataSource
import com.mulin.us.data.remote.SyncRemoteDataSource
import com.mulin.us.data.remote.cloudbase.CloudBaseAuthDataSource
import com.mulin.us.data.remote.cloudbase.CloudBaseCoupleRemoteDataSource
import com.mulin.us.data.remote.cloudbase.CloudBaseGateway
import com.mulin.us.data.remote.cloudbase.CloudBaseMemoryRemoteDataSource
import com.mulin.us.data.remote.cloudbase.CloudBaseStorageDataSource
import com.mulin.us.data.remote.cloudbase.CloudBaseSyncRemoteDataSource
import com.mulin.us.data.remote.cloudbase.TokenHolder
import com.mulin.us.data.remote.offline.FakeSyncRemoteDataSource
import com.mulin.us.data.remote.offline.LocalStorageDataSource
import com.mulin.us.data.remote.offline.OfflineAuthDataSource
import com.mulin.us.data.remote.offline.OfflineCoupleRemoteDataSource
import com.mulin.us.data.remote.offline.OfflineMemoryRemoteDataSource
import com.mulin.us.data.repository.impl.AuthRepositoryImpl
import com.mulin.us.data.repository.impl.CoupleRepositoryImpl
import com.mulin.us.data.repository.impl.DiaryRepositoryImpl
import com.mulin.us.data.repository.impl.MeetingPlanRepositoryImpl
import com.mulin.us.data.repository.impl.MemoryRepositoryImpl
import com.mulin.us.data.repository.impl.SyncRepositoryImpl
import com.mulin.us.data.repository.impl.WishRepositoryImpl
import com.mulin.us.data.sync.OutboxWriter
import com.mulin.us.data.sync.SyncCoordinator
import com.mulin.us.data.sync.SyncScheduler
import com.mulin.us.domain.repository.AuthRepository
import com.mulin.us.domain.repository.CoupleRepository
import com.mulin.us.domain.repository.DiaryRepository
import com.mulin.us.domain.repository.MeetingPlanRepository
import com.mulin.us.domain.repository.MemoryRepository
import com.mulin.us.domain.repository.SyncRepository
import com.mulin.us.domain.repository.WishRepository

/**
 * 手写依赖容器（不用 Hilt）。
 *
 * 为什么不用 Hilt：
 * 两个用户、十来张表的项目，注解处理器带来的编译时间和心智负担大于收益；
 * 手写一个类还能一眼看完"谁依赖谁"，出问题好查。
 *
 * 关键分支在最后：cloudEnabled 决定注入"真网络实现"还是"本地替身"。
 * 所以「没配置环境 = 0 网络请求 = 0 费用」不是一个口号，是构造期就定死的事实。
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val db: YuniDatabase = YuniDatabase.get(appContext)
    val vault: TokenVault = TokenVault(appContext)

    private val cloudEnabled: Boolean = AppConfig.cloudEnabled

    /** 只在云端模式下才会被创建；本地模式连这个对象都不存在 */
    private val gateway: CloudBaseGateway by lazy { CloudBaseGateway(vault) }

    val authDataSource: AuthenticationDataSource =
        if (cloudEnabled) CloudBaseAuthDataSource(gateway, vault) else OfflineAuthDataSource()

    val coupleRemote: CoupleRemoteDataSource =
        if (cloudEnabled) CloudBaseCoupleRemoteDataSource(gateway) else OfflineCoupleRemoteDataSource()

    val memoryRemote: MemoryRemoteDataSource =
        if (cloudEnabled) CloudBaseMemoryRemoteDataSource(gateway) else OfflineMemoryRemoteDataSource()

    val syncRemote: SyncRemoteDataSource =
        if (cloudEnabled) CloudBaseSyncRemoteDataSource(gateway) else FakeSyncRemoteDataSource()

    val storage: StorageDataSource =
        if (cloudEnabled) CloudBaseStorageDataSource(gateway) else LocalStorageDataSource(appContext)

    val outbox: OutboxWriter = OutboxWriter(db.syncTaskDao(), db.syncCursorDao())

    val syncCoordinator: SyncCoordinator = SyncCoordinator(db, syncRemote, outbox)

    val syncScheduler: SyncScheduler = SyncScheduler(appContext)

    // ---------------- 仓储 ----------------

    val authRepository: AuthRepository = AuthRepositoryImpl(db, authDataSource, vault)

    val coupleRepository: CoupleRepository =
        CoupleRepositoryImpl(db, coupleRemote, authRepository)

    val memoryRepository: MemoryRepository = MemoryRepositoryImpl(db, outbox)

    val diaryRepository: DiaryRepository = DiaryRepositoryImpl(db, outbox)

    val wishRepository: WishRepository = WishRepositoryImpl(db, outbox)

    val meetingPlanRepository: MeetingPlanRepository = MeetingPlanRepositoryImpl(db, outbox)

    val syncRepository: SyncRepository = SyncRepositoryImpl(db, syncCoordinator)

    /** App 启动时调一次：把已登录用户的 uid 放回内存，供拼查询条件用 */
    fun restoreSession() {
        if (!cloudEnabled) return
        TokenHolder.uid = vault.userId()
    }

    fun cloudStateText(): String = when {
        !cloudEnabled -> "本地模式（未配置环境，0 请求）"
        vault.hasSession() -> "云端已配置，已登录"
        else -> "云端已配置，未登录"
    }
}
