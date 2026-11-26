package com.mulin.us.core

import android.content.Context
import kotlinx.coroutines.launch
import com.mulin.us.core.common.AppConfig
import com.mulin.us.core.security.TokenVault
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
import com.mulin.us.data.remote.cloudbase.CloudSessionState
import com.mulin.us.data.remote.cloudbase.TokenHolder
import com.mulin.us.data.remote.ecs.EcsAuthDataSource
import com.mulin.us.data.remote.ecs.EcsCoupleRemoteDataSource
import com.mulin.us.data.remote.ecs.EcsGate
import com.mulin.us.data.remote.ecs.EcsMemoryRemoteDataSource
import com.mulin.us.data.remote.ecs.EcsStorageDataSource
import com.mulin.us.data.remote.ecs.EcsSyncRemoteDataSource
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
    private val ecsEnabled: Boolean = AppConfig.ecsEnabled

    /** 只在云端模式下才会被创建；本地模式连这个对象都不存在 */
    private val gateway: CloudBaseGateway by lazy { CloudBaseGateway(vault) }
    private val ecsGate: EcsGate by lazy { EcsGate(AppConfig.ecsBaseUrl, vault) }

    // 五种云端实现的选择：ECS 跳板 > CloudBase > 本地替身。
    // 换回 CloudBase / 到期丢弃 ECS：把 ecsEnabled 分支去掉即可，其余代码零改动。
    val authDataSource: AuthenticationDataSource = when {
        ecsEnabled -> EcsAuthDataSource(ecsGate, vault)
        cloudEnabled -> CloudBaseAuthDataSource(gateway, vault)
        else -> OfflineAuthDataSource()
    }

    val coupleRemote: CoupleRemoteDataSource = when {
        ecsEnabled -> EcsCoupleRemoteDataSource(ecsGate, vault)
        cloudEnabled -> CloudBaseCoupleRemoteDataSource(gateway)
        else -> OfflineCoupleRemoteDataSource()
    }

    val memoryRemote: MemoryRemoteDataSource = when {
        ecsEnabled -> EcsMemoryRemoteDataSource(ecsGate)
        cloudEnabled -> CloudBaseMemoryRemoteDataSource(gateway)
        else -> OfflineMemoryRemoteDataSource()
    }

    val syncRemote: SyncRemoteDataSource = when {
        ecsEnabled -> EcsSyncRemoteDataSource(ecsGate)
        cloudEnabled -> CloudBaseSyncRemoteDataSource(gateway)
        else -> FakeSyncRemoteDataSource()
    }

    val storage: StorageDataSource = when {
        ecsEnabled -> EcsStorageDataSource()
        cloudEnabled -> CloudBaseStorageDataSource(gateway)
        else -> LocalStorageDataSource(appContext)
    }

    val outbox: OutboxWriter = OutboxWriter(db.syncTaskDao(), db.syncCursorDao())

    val syncCoordinator: SyncCoordinator = SyncCoordinator(db, syncRemote, outbox)

    val syncScheduler: SyncScheduler = SyncScheduler(appContext)
    val reminderScheduler: com.mulin.us.data.reminder.ReminderScheduler =
        com.mulin.us.data.reminder.ReminderScheduler(appContext)
    val imageStore: com.mulin.us.data.remote.ecs.ImageStore =
        com.mulin.us.data.remote.ecs.ImageStore(appContext, ecsGate)
    val letterRepository: com.mulin.us.domain.repository.LetterRepository =
        com.mulin.us.data.repository.impl.EcsLetterRepository(ecsGate, db)
    val memoryPhotoRepository: com.mulin.us.domain.repository.MemoryPhotoRepository =
        com.mulin.us.data.repository.impl.ImageRepositoryImpl(imageStore, db.memoryPhotoDao())

    // ---------------- 仓储 ----------------

    val avatarStore: com.mulin.us.data.local.AvatarStore =
        com.mulin.us.data.local.AvatarStore(appContext)

    val authRepository: AuthRepository = AuthRepositoryImpl(db, authDataSource, vault, avatarStore)

    val coupleRepository: CoupleRepository =
        CoupleRepositoryImpl(db, coupleRemote, authRepository)

    val memoryRepository: MemoryRepository = MemoryRepositoryImpl(db, outbox)

    val diaryRepository: DiaryRepository = DiaryRepositoryImpl(db, outbox)

    val wishRepository: WishRepository = WishRepositoryImpl(db, outbox)

    val meetingPlanRepository: MeetingPlanRepository = MeetingPlanRepositoryImpl(db, outbox)

    val syncRepository: SyncRepository = SyncRepositoryImpl(db, syncCoordinator)

    // S5：ECS 模式下才创建；本地/offline 模式给出明确失败，不假装成功
    val questionRepository: com.mulin.us.domain.repository.QuestionRepository =
        com.mulin.us.data.repository.impl.EcsQuestionRepository(ecsGate, db)
    val messageRepository: com.mulin.us.domain.repository.MessageRepository =
        com.mulin.us.data.repository.impl.EcsMessageRepository(ecsGate)
    val commentRepository: com.mulin.us.domain.repository.CommentRepository =
        com.mulin.us.data.repository.impl.EcsCommentRepository(ecsGate)

    /** 时间线聚合（工单 V3.1 B6）：首页与回忆页共用，不建主表 */
    val timelineRepository: com.mulin.us.domain.repository.TimelineRepository =
        com.mulin.us.data.repository.impl.TimelineRepositoryImpl(
            memoryRepository,
            diaryRepository,
            meetingPlanRepository,
            wishRepository,
            questionRepository,
            letterRepository,
            coupleRepository,
            memoryPhotoRepository
        )

    // UseCase（路线图 1.5）：把散落在 ViewModel 的业务规则收口到这里
    val createMemoryUseCase = com.mulin.us.domain.usecase.CreateMemoryUseCase(memoryRepository)
    val writeDiaryUseCase = com.mulin.us.domain.usecase.WriteDiaryUseCase(diaryRepository)
    val syncNowUseCase = com.mulin.us.domain.usecase.SyncNowUseCase(syncRepository)

    /** App 启动时调一次：把已登录用户的 uid 放回内存，供拼查询条件用 */
    fun restoreSession() {
        if (!AppConfig.networkEnabled) return
        TokenHolder.uid = vault.userId()
        if (ecsEnabled && !vault.coupleId().isNullOrBlank()) {
            CloudSessionState.coupleId = vault.coupleId()
        }
        // 兼容旧版本：vault 没存 coupleId 但服务端已绑定时，异步拉一次 /me 补齐。
        // 只在启动时发一次，不在每次同步请求里嵌套发（避免 OkHttp unexpected end of stream）。
        if (ecsEnabled && vault.hasSession()) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                if (vault.coupleId().isNullOrBlank()) {
                    when (val r = authDataSource.fetchMe()) {
                        is com.mulin.us.data.remote.RemoteResult.Success -> {
                            r.data["coupleId"]?.takeIf { it.isNotBlank() }?.let {
                                vault.saveCoupleId(it)
                                com.mulin.us.data.remote.cloudbase.CloudSessionState.coupleId = it
                            }
                        }
                        else -> Unit
                    }
                }
                // 工单 C3.8：双端日期同步。只在服务端更新时覆盖本地，失败静默（本地数据可用）
                coupleRepository.refreshStartDateFromRemote()
            }
        }
    }

    fun cloudStateText(): String = when {
        ecsEnabled -> "ECS 跳板（临时，${if (vault.hasSession()) "已登录" else "未登录"}）"
        !cloudEnabled -> "本地模式（未配置环境，0 请求）"
        vault.hasSession() -> "云端已配置，已登录"
        else -> "云端已配置，未登录"
    }
}
