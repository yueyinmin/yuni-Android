package com.mulin.us.data.repository.impl

import com.mulin.us.core.common.AppConfig
import com.mulin.us.core.common.SafeLog
import com.mulin.us.core.security.TokenVault
import com.mulin.us.data.local.AvatarStore
import com.mulin.us.data.local.toEntity
import com.mulin.us.data.local.toModel
import com.mulin.us.data.local.YuniDatabase
import com.mulin.us.data.local.entity.CoupleEntity
import com.mulin.us.data.local.entity.DiaryEntity
import com.mulin.us.data.local.entity.LocalUserEntity
import com.mulin.us.data.local.entity.MeetingPlanEntity
import com.mulin.us.data.local.entity.MemoryEntity
import com.mulin.us.data.local.entity.WishEntity
import com.mulin.us.data.remote.AuthenticationDataSource
import com.mulin.us.data.remote.CoupleRemoteDataSource
import com.mulin.us.data.remote.RemoteResult
import com.mulin.us.data.remote.cloudbase.CloudSessionState
import com.mulin.us.data.remote.cloudbase.TokenHolder
import com.mulin.us.data.sync.OutboxWriter
import com.mulin.us.data.sync.SyncCoordinator
import com.mulin.us.data.sync.SyncStateStore
import com.mulin.us.domain.model.CoupleProfile
import com.mulin.us.domain.model.Diary
import com.mulin.us.domain.model.DiaryVisibility
import com.mulin.us.domain.model.LocalUser
import com.mulin.us.domain.model.MeetingPlan
import com.mulin.us.domain.model.Memory
import com.mulin.us.domain.model.SyncOperation
import com.mulin.us.domain.model.SyncStatus
import com.mulin.us.domain.model.Wish
import com.mulin.us.domain.repository.AuthRepository
import com.mulin.us.domain.repository.CoupleRepository
import com.mulin.us.domain.repository.DiaryRepository
import com.mulin.us.domain.repository.MeetingPlanRepository
import com.mulin.us.domain.repository.MemoryRepository
import com.mulin.us.domain.repository.SyncRepository
import com.mulin.us.domain.repository.WishRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import android.net.Uri
import java.util.UUID

/**
 * 仓储实现。
 *
 * 所有"要不要上云"的判断都集中在这里，不让它散落到 ViewModel 或 DAO：
 * - 私密日记 → LOCAL_ONLY，永不入队（工单硬要求）；
 * - 共同内容 → PENDING_UPLOAD + 入队；
 * - 本地模式（没配环境）→ 一律 LOCAL_ONLY，不产生任何网络请求。
 */

class AuthRepositoryImpl(
    private val db: YuniDatabase,
    private val authRemote: AuthenticationDataSource,
    private val vault: TokenVault,
    private val avatarStore: AvatarStore
) : AuthRepository {

    override val currentUser: Flow<LocalUser?> =
        db.userDao().observeCurrent().map { it?.toModel() }

    override val isLoggedIn: Flow<Boolean> =
        db.userDao().observeCurrent().map { it != null }

    override suspend fun register(username: String, password: String): Result<LocalUser> =
        authenticate(username, password, isRegister = true)

    override suspend fun login(username: String, password: String): Result<LocalUser> =
        authenticate(username, password, isRegister = false)

    private suspend fun authenticate(
        username: String,
        password: String,
        isRegister: Boolean
    ): Result<LocalUser> {
        // 先把"能不能联网"这件事拦在最前面：没配环境时连网络请求都不发
        if (!AppConfig.networkEnabled) {
            return Result.failure(
                IllegalStateException("本地模式：未配置 CloudBase 环境。可以先用「创建本机档案」离线使用。")
            )
        }
        val result = if (isRegister) authRemote.register(username, password)
        else authRemote.login(username, password)

        return when (result) {
            is RemoteResult.Success -> {
                val uid = result.data
                TokenHolder.uid = uid
                val entity = LocalUserEntity(
                    id = uid,
                    username = username,
                    nickname = username,
                    createdAt = System.currentTimeMillis()
                )
                db.userDao().upsert(entity)
                SafeLog.i("Auth", "authenticated uid=${SafeLog.mask(uid)}")
                Result.success(entity.toModel())
            }
            else -> Result.failure(IllegalStateException(describe(result)))
        }
    }

    override suspend fun logout() {
        // 只清登录态；本地日记、本地回忆一个字都不动（工单：退出登录不能删本地日记）
        authRemote.logout()
        TokenHolder.uid = null
        CloudSessionState.coupleId = null
        // 注意：这里故意没有 db.diaryDao().clear() 之类的调用
        SafeLog.i("Auth", "logout: local data kept")
    }

    override suspend fun createLocalProfile(nickname: String): LocalUser {
        // 修复：之前每次打开档案弹窗都会生成新 id，顺手抹掉已经选好的头像。
        val existing = db.userDao().current()
        val entity = if (existing != null) {
            existing.copy(nickname = nickname.ifBlank { existing.nickname })
        } else {
            LocalUserEntity(
                id = "local-" + UUID.randomUUID(),
                username = "local",
                nickname = nickname.ifBlank { "我" },
                createdAt = System.currentTimeMillis()
            )
        }
        db.userDao().upsert(entity)
        SafeLog.i("Auth", "local profile saved id=${SafeLog.mask(entity.id)}")
        return entity.toModel()
    }

    override suspend fun saveAvatar(uri: Uri): Result<LocalUser> = runCatching {
        val current = db.userDao().current() ?: run {
            createLocalProfile("我")
            db.userDao().current()!!
        }
        val path = avatarStore.save(uri, current.id).getOrThrow()
        val updated = current.copy(avatarLocalPath = path)
        db.userDao().upsert(updated)
        SafeLog.i("Auth", "avatar saved")
        updated.toModel()
    }

    override suspend fun saveNickname(nickname: String): Result<LocalUser> = runCatching {
        createLocalProfile(nickname)
    }

    private fun describe(r: RemoteResult<*>): String = when (r) {
        is RemoteResult.NotConfigured -> r.message
        is RemoteResult.Network -> "网络不可用：" + r.message
        is RemoteResult.Unauthorized -> "账号或密码不正确，或登录已过期"
        is RemoteResult.Forbidden -> "云端拒绝访问：" + r.message
        is RemoteResult.Conflict -> "数据版本冲突"
        is RemoteResult.BadRequest -> "请求有误：" + r.message
        is RemoteResult.Server -> "云端异常：" + r.message
        is RemoteResult.Success -> "成功"
    }
}

class CoupleRepositoryImpl(
    private val db: YuniDatabase,
    private val coupleRemote: CoupleRemoteDataSource,
    private val authRepository: AuthRepository
) : CoupleRepository {

    override val couple: Flow<CoupleProfile?> =
        db.coupleDao().observeActive().map { it?.toModel() }

    override suspend fun createLocalCouple(relationshipStartEpochDay: Long?): CoupleProfile {
        val me = db.userDao().current()?.id ?: "local-me"
        val entity = CoupleEntity(
            id = UUID.randomUUID().toString(),
            memberAId = me,
            memberBId = null,
            relationshipStartEpochDay = relationshipStartEpochDay,
            status = "active",
            createdAt = System.currentTimeMillis()
        )
        db.coupleDao().upsert(entity)
        return entity.toModel()
    }

    override suspend fun createCloudCouple(relationshipStartEpochDay: Long?): Result<CoupleProfile> =
        when (val r = coupleRemote.createCouple(relationshipStartEpochDay)) {
            is RemoteResult.Success -> {
                val entity = CoupleEntity(
                    id = r.data.id,
                    memberAId = r.data.memberAId,
                    memberBId = r.data.memberBId,
                    relationshipStartEpochDay = r.data.relationshipStartEpochDay,
                    status = r.data.status,
                    createdAt = System.currentTimeMillis()
                )
                db.coupleDao().upsert(entity)
                CloudSessionState.coupleId = r.data.id
                Result.success(entity.toModel())
            }
            else -> Result.failure(IllegalStateException(errorText(r)))
        }

    override suspend fun createInvite(): Result<String> =
        when (val r = coupleRemote.createInvite()) {
            is RemoteResult.Success -> Result.success(r.data)
            else -> Result.failure(IllegalStateException(errorText(r)))
        }

    override suspend fun acceptInvite(code: String): Result<Unit> =
        when (val r = coupleRemote.acceptInvite(code)) {
            is RemoteResult.Success -> {
                db.coupleDao().upsert(
                    CoupleEntity(
                        id = r.data.id,
                        memberAId = r.data.memberAId,
                        memberBId = r.data.memberBId,
                        relationshipStartEpochDay = r.data.relationshipStartEpochDay,
                        status = r.data.status,
                        createdAt = System.currentTimeMillis()
                    )
                )
                Result.success(Unit)
            }
            else -> Result.failure(IllegalStateException(errorText(r)))
        }

    override suspend fun setRelationshipStart(epochDay: Long) {
        db.coupleDao().active()?.let { db.coupleDao().setRelationshipStart(it.id, epochDay) }
    }

    override suspend fun setStartDateIso(iso: String) {
        val date = runCatching { java.time.LocalDate.parse(iso) }.getOrNull() ?: return
        val existing = db.coupleDao().active()
        val id = existing?.id ?: run {
            // 工单 C3.8：未绑定时允许先保存在本地 —— 没有空间就建一个本地空间行
            val localId = "local-${java.util.UUID.randomUUID().toString().take(12)}"
            db.coupleDao().upsert(
                com.mulin.us.data.local.entity.CoupleEntity(
                    id = localId,
                    memberAId = db.userDao().current()?.id ?: "local-me",
                    startDateIso = iso,
                    relationshipStartEpochDay = date.toEpochDay(),
                    createdAt = System.currentTimeMillis()
                )
            )
            localId
        }
        db.coupleDao().setStartDateIso(id, iso, date.toEpochDay())
    }

    override suspend fun dissolve(): Result<Unit> {
        val current = db.coupleDao().active()
            ?: return Result.success(Unit)
        // 只把共同空间标成解散；本地私密日记、个人设置全部保留
        db.coupleDao().markDissolved(current.id)
        CloudSessionState.coupleId = null
        SafeLog.i("Couple", "dissolved locally, private diaries untouched")
        return Result.success(Unit)
    }

    private fun errorText(r: RemoteResult<*>): String = when (r) {
        is RemoteResult.NotConfigured -> r.message
        is RemoteResult.Network -> "网络不可用"
        is RemoteResult.Unauthorized -> "请先登录"
        is RemoteResult.Forbidden -> "没有权限"
        is RemoteResult.Conflict -> "状态冲突"
        is RemoteResult.BadRequest -> "参数有误：" + r.message
        is RemoteResult.Server -> "云端异常：" + r.message
        is RemoteResult.Success -> ""
    }
}

class MemoryRepositoryImpl(
    private val db: YuniDatabase,
    private val outbox: OutboxWriter
) : MemoryRepository {

    override fun observeAll(): Flow<List<Memory>> =
        db.memoryDao().observeActive().map { list -> list.map { it.toModel() } }

    override fun observeCount(): Flow<Int> = db.memoryDao().observeCount()

    override suspend fun add(title: String, content: String, happenedAt: Long?, mood: String?): Long {
        val now = System.currentTimeMillis()
        val mutationId = UUID.randomUUID().toString()
        val canSync = AppConfig.networkEnabled
        val entity = MemoryEntity(
            cloudId = null,
            title = title,
            content = content,
            happenedAt = happenedAt,
            mood = mood,
            createdAt = now,
            updatedAt = now,
            syncStatus = if (canSync) SyncStatus.PENDING_UPLOAD.name else SyncStatus.LOCAL_ONLY.name,
            clientMutationId = if (canSync) mutationId else null
        )
        val localId = db.memoryDao().insert(entity)
        if (canSync) {
            outbox.enqueueMemory(entity.copy(localId = localId), SyncOperation.CREATE, mutationId)
        }
        SafeLog.eLength("Memory", "created title", title.length)
        return localId
    }

    override suspend fun update(model: Memory) {
        val existing = db.memoryDao().byId(model.localId) ?: return
        val mutationId = outbox.effectiveMutationId("memory", model.localId, UUID.randomUUID().toString())
        val canSync = AppConfig.networkEnabled
        val updated = existing.copy(
            title = model.title,
            content = model.content,
            happenedAt = model.happenedAt,
            mood = model.mood,
            updatedAt = System.currentTimeMillis(),
            syncStatus = if (canSync) SyncStatus.PENDING_UPLOAD.name else existing.syncStatus,
            clientMutationId = if (canSync) mutationId else existing.clientMutationId
        )
        db.memoryDao().update(updated)
        if (canSync) outbox.enqueueMemory(updated, SyncOperation.UPDATE, mutationId)
    }

    override suspend fun softDelete(localId: Long) {
        val existing = db.memoryDao().byId(localId) ?: return
        db.memoryDao().softDelete(localId, System.currentTimeMillis())
        if (AppConfig.networkEnabled) {
            outbox.enqueueMemory(
                existing.copy(deletedAt = System.currentTimeMillis()),
                SyncOperation.DELETE,
                UUID.randomUUID().toString()
            )
        }
    }
}

class DiaryRepositoryImpl(
    private val db: YuniDatabase,
    private val outbox: OutboxWriter
) : DiaryRepository {

    /**
     * 当前空间 ID。优先取云端会话，没联网时退回本机记录。
     * 拿不到就返回 null —— 宁可这行先不上云，也不能替它猜一个空间。
     */
    private suspend fun currentCoupleId(): String? =
        CloudSessionState.coupleId ?: db.coupleDao().active()?.id

    override fun observeAll(): Flow<List<Diary>> =
        db.diaryDao().observeActive().map { list -> list.map { it.toModel() } }

    override fun observeByVisibility(visibility: DiaryVisibility): Flow<List<Diary>> =
        db.diaryDao().observeByVisibility(visibility.name).map { list -> list.map { it.toModel() } }

    override fun observeCount(): Flow<Int> = db.diaryDao().observeCount()

    override suspend fun add(
        ownerId: String,
        visibility: DiaryVisibility,
        title: String?,
        content: String,
        entryEpochDay: Long,
        mood: String?
    ): Long {
        val now = System.currentTimeMillis()
        // 私密日记不上云；共享日记没认领空间时也先留在本机（迁移来的历史行就是这种）
        val coupleId = if (visibility == DiaryVisibility.SHARED) currentCoupleId() else null
        val syncable = visibility == DiaryVisibility.SHARED &&
            AppConfig.networkEnabled && !coupleId.isNullOrBlank()
        val mutationId = if (syncable) UUID.randomUUID().toString() else null
        val entity = DiaryEntity(
            cloudId = null,
            coupleId = coupleId,
            ownerId = ownerId,
            visibility = visibility.name,
            title = title,
            content = content,
            entryEpochDay = entryEpochDay,
            mood = mood,
            createdAt = now,
            updatedAt = now,
            syncStatus = if (syncable) SyncStatus.PENDING_UPLOAD.name else SyncStatus.LOCAL_ONLY.name,
            clientMutationId = mutationId
        )
        val localId = db.diaryDao().insert(entity)
        if (syncable && mutationId != null) {
            val payload = JSONObject()
                .put("client_mutation_id", mutationId)
                .put("visibility", visibility.name)
                .put("title", title ?: JSONObject.NULL)
                .put("content", content)
                .put("entry_epoch_day", entryEpochDay)
                .put("owner_id", ownerId)
            outbox.enqueueDiary(localId, visibility.name, coupleId, payload, SyncOperation.CREATE, mutationId)
        }
        SafeLog.eLength("Diary", "created content", content.length)
        return localId
    }

    override suspend fun update(model: Diary) {
        val existing = db.diaryDao().byId(model.localId) ?: return
        // 空间归属沿用原值，不允许通过 update 把一条数据搬到别的空间
        val coupleId = existing.coupleId
        val syncable = model.visibility == DiaryVisibility.SHARED &&
            AppConfig.networkEnabled && !coupleId.isNullOrBlank()
        val mutationId = if (syncable) outbox.effectiveMutationId("diary", model.localId, UUID.randomUUID().toString()) else existing.clientMutationId
        val updated = existing.copy(
            title = model.title,
            content = model.content,
            visibility = model.visibility.name,
            mood = model.mood,
            updatedAt = System.currentTimeMillis(),
            syncStatus = if (syncable) SyncStatus.PENDING_UPLOAD.name
            else if (model.visibility == DiaryVisibility.PRIVATE) SyncStatus.LOCAL_ONLY.name
            else existing.syncStatus,
            clientMutationId = mutationId
        )
        db.diaryDao().update(updated)
        if (syncable && mutationId != null) {
            val payload = JSONObject()
                .put("client_mutation_id", mutationId)
                .put("visibility", model.visibility.name)
                .put("title", model.title ?: JSONObject.NULL)
                .put("content", model.content)
                .put("entry_epoch_day", model.entryEpochDay)
                .put("owner_id", model.ownerId)
            outbox.enqueueDiary(model.localId, model.visibility.name, coupleId, payload, SyncOperation.UPDATE, mutationId)
        }
    }

    override suspend fun softDelete(localId: Long) {
        val existing = db.diaryDao().byId(localId) ?: return
        db.diaryDao().softDelete(localId, System.currentTimeMillis())
        // 只有"已认领空间的共享日记"需要通知云端删除；私密日记和历史遗留数据删了就删了
        val coupleId = existing.coupleId
        if (existing.visibility == DiaryVisibility.SHARED.name &&
            AppConfig.networkEnabled && !coupleId.isNullOrBlank()
        ) {
            outbox.enqueueDiary(
                localId = localId,
                visibility = existing.visibility,
                coupleId = coupleId,
                payload = JSONObject().put("deleted_at", System.currentTimeMillis()),
                operation = SyncOperation.DELETE,
                mutationId = UUID.randomUUID().toString()
            )
        }
    }
}

class WishRepositoryImpl(
    private val db: YuniDatabase,
    private val outbox: OutboxWriter
) : WishRepository {

    override fun observeAll(): Flow<List<Wish>> =
        db.wishDao().observeActive().map { list -> list.map { it.toModel() } }

    override fun observeCount(): Flow<Int> = db.wishDao().observeCount()

    override suspend fun add(title: String, note: String?, targetAt: Long?): Long {
        val canSync = AppConfig.networkEnabled
        val mutationId = if (canSync) UUID.randomUUID().toString() else null
        val entity = WishEntity(
            title = title,
            note = note,
            targetAt = targetAt,
            createdAt = System.currentTimeMillis(),
            syncStatus = if (canSync) SyncStatus.PENDING_UPLOAD.name else SyncStatus.LOCAL_ONLY.name,
            clientMutationId = mutationId
        )
        val id = db.wishDao().insert(entity)
        if (canSync && mutationId != null) {
            outbox.enqueue(
                "wish",
                id,
                SyncOperation.CREATE,
                JSONObject()
                    .put("client_mutation_id", mutationId)
                    .put("title", title)
                    .put("note", note ?: JSONObject.NULL)
                    .put("target_at", targetAt ?: JSONObject.NULL)
                    .toString(),
                mutationId
            )
        }
        return id
    }

    override suspend fun markDone(localId: Long, done: Boolean) {
        db.wishDao().setDone(localId, if (done) System.currentTimeMillis() else null, System.currentTimeMillis())
    }

    override suspend fun softDelete(localId: Long) {
        db.wishDao().softDelete(localId, System.currentTimeMillis())
    }
}

class MeetingPlanRepositoryImpl(
    private val db: YuniDatabase,
    private val outbox: OutboxWriter
) : MeetingPlanRepository {

    override fun observeAll(): Flow<List<MeetingPlan>> =
        db.meetingPlanDao().observeActive().map { list -> list.map { it.toModel() } }

    override suspend fun add(plannedAt: Long, place: String?, note: String?): Long {
        val canSync = AppConfig.networkEnabled
        val mutationId = if (canSync) UUID.randomUUID().toString() else null
        val entity = MeetingPlanEntity(
            plannedAt = plannedAt,
            place = place,
            note = note,
            createdAt = System.currentTimeMillis(),
            syncStatus = if (canSync) SyncStatus.PENDING_UPLOAD.name else SyncStatus.LOCAL_ONLY.name,
            clientMutationId = mutationId
        )
        val id = db.meetingPlanDao().insert(entity)
        if (canSync && mutationId != null) {
            outbox.enqueue(
                "meeting_plan",
                id,
                SyncOperation.CREATE,
                JSONObject()
                    .put("client_mutation_id", mutationId)
                    .put("planned_at", plannedAt)
                    .put("place", place ?: JSONObject.NULL)
                    .put("note", note ?: JSONObject.NULL)
                    .toString(),
                mutationId
            )
        }
        return id
    }

    override suspend fun softDelete(localId: Long) {
        db.meetingPlanDao().softDelete(localId, System.currentTimeMillis())
    }
}

class SyncRepositoryImpl(
    private val db: YuniDatabase,
    private val coordinator: SyncCoordinator
) : SyncRepository {

    override fun observePendingCount(): Flow<Int> = db.syncTaskDao().observePendingCount()

    override fun observeLastSyncAt(): Flow<Long> = SyncStateStore.lastSyncAt

    override fun observeLastResult(): Flow<String> = SyncStateStore.lastResult

    override suspend fun syncNow(reason: String) {
        coordinator.runOnce(reason)
    }

    override val cloudAvailable: Boolean get() = AppConfig.networkEnabled
}
