package com.mulin.us.domain.repository

import com.mulin.us.domain.model.CoupleProfile
import com.mulin.us.domain.model.Diary
import com.mulin.us.domain.model.Comment
import com.mulin.us.domain.model.DailyQuestion
import com.mulin.us.domain.model.DiaryVisibility
import com.mulin.us.domain.model.Letter
import com.mulin.us.domain.model.MemoryPhoto
import com.mulin.us.domain.model.LocalUser
import com.mulin.us.domain.model.Message
import com.mulin.us.domain.model.MeetingPlan
import com.mulin.us.domain.model.Memory
import com.mulin.us.domain.model.SyncStatus
import com.mulin.us.domain.model.Wish
import kotlinx.coroutines.flow.Flow

/**
 * 仓储层接口（Domain 层）。
 *
 * 铁律：
 * - ViewModel 只依赖这些接口，永远看不到 OkHttp、Room、CloudBase；
 * - 实现类放在 data/repository/impl；
 * - 云端换了（CloudBase → 别家），这些接口一个字都不用改。
 */

interface AuthRepository {
    val currentUser: Flow<LocalUser?>
    val isLoggedIn: Flow<Boolean>
    suspend fun register(username: String, password: String): Result<LocalUser>
    suspend fun login(username: String, password: String): Result<LocalUser>
    /** 退出登录：只清 token 和内存态，绝不动本地日记（工单硬要求） */
    suspend fun logout()
    /** 本机离线建档（还没登录也能用 App） */
    suspend fun createLocalProfile(nickname: String): LocalUser
}

interface CoupleRepository {
    val couple: Flow<CoupleProfile?>
    suspend fun createLocalCouple(relationshipStartEpochDay: Long?): CoupleProfile
    /** 云端建空间：A 登录后调用，拿到 coupleId 后才能发邀请码 */
    suspend fun createCloudCouple(relationshipStartEpochDay: Long?): Result<CoupleProfile>
    /** 生成邀请码：只有云端模式且已登录才可能成功；本地模式返回 NotConfigured */
    suspend fun createInvite(): Result<String>
    suspend fun acceptInvite(code: String): Result<Unit>
    /** 设置恋爱开始日（epochDay）；S4a 相恋天数依赖它 */
    suspend fun setRelationshipStart(epochDay: Long)
    /** 工单 V3.1 C3.5：ISO 日期写入（权威），同时兼容旧 epochDay 列 */
    suspend fun setStartDateIso(iso: String)
    /** 解散共同空间：只影响共享数据，本地私密日记必须原样保留 */
    suspend fun dissolve(): Result<Unit>
}

interface MemoryRepository {
    fun observeAll(): Flow<List<Memory>>
    /** 只取条数（不加载正文）。放在 Repository 而不是让 UI 直接查 DAO —— 见 B0.1 T03 依赖边界第 7 条 */
    fun observeCount(): Flow<Int>
    suspend fun add(title: String, content: String, happenedAt: Long?, mood: String? = null): Long
    suspend fun update(model: Memory)
    suspend fun softDelete(localId: Long)
}

interface DiaryRepository {
    fun observeAll(): Flow<List<Diary>>
    fun observeByVisibility(visibility: DiaryVisibility): Flow<List<Diary>>
    fun observeCount(): Flow<Int>
    suspend fun add(
        ownerId: String,
        visibility: DiaryVisibility,
        title: String?,
        content: String,
        entryEpochDay: Long,
        mood: String? = null
    ): Long
    suspend fun update(model: Diary)
    suspend fun softDelete(localId: Long)
}

interface WishRepository {
    fun observeAll(): Flow<List<Wish>>
    fun observeCount(): Flow<Int>
    suspend fun add(title: String, note: String?, targetAt: Long?): Long
    suspend fun markDone(localId: Long, done: Boolean)
    suspend fun softDelete(localId: Long)
}

interface MeetingPlanRepository {
    fun observeAll(): Flow<List<MeetingPlan>>
    suspend fun add(plannedAt: Long, place: String?, note: String?): Long
    suspend fun softDelete(localId: Long)
}

/** S5 每日问答（服务端控制揭晓） */
interface QuestionRepository {
    suspend fun today(epochDay: Long): Result<DailyQuestion>
    suspend fun answer(epochDay: Long, answer: String): Result<Boolean>
    suspend fun history(limit: Int = 30): Result<List<DailyQuestion>>
}

/** S5 悄悄话 */
interface MessageRepository {
    suspend fun send(content: String): Result<Message>
    suspend fun list(limit: Int = 50): Result<List<Message>>
    suspend fun markRead(id: String): Result<Unit>
    suspend fun unreadCount(): Result<Int>
}

/** 4.8/4.9 回忆照片 */
interface MemoryPhotoRepository {
    /** memoryLocalId 是 Room 外键；memoryCloudId 用于上传；二者都要 */
    suspend fun add(memoryLocalId: Long, memoryCloudId: String, uri: android.net.Uri): Result<MemoryPhoto>
    /** 持久化查询：重启 App 后缩略图还在 */
    fun observeByMemory(memoryLocalId: Long): kotlinx.coroutines.flow.Flow<List<MemoryPhoto>>
}

/** S6 未来信 */
interface LetterRepository {
    suspend fun create(title: String, content: String, unlockAt: Long, kind: String = "LETTER"): Result<Letter>
    suspend fun list(): Result<List<Letter>>
    suspend fun open(id: String): Result<String>
}

/** 5.6 回忆评论 */
interface CommentRepository {
    suspend fun list(memoryCloudId: String): Result<List<Comment>>
    suspend fun add(memoryCloudId: String, content: String): Result<Comment>
}

/** 同步门面：UI 只调这几个方法，不碰 Worker */
interface SyncRepository {
    fun observePendingCount(): Flow<Int>
    fun observeLastSyncAt(): Flow<Long>
    fun observeLastResult(): Flow<String>
    suspend fun syncNow(reason: String)
    /** 当前是否处于"云端可用"状态；本地模式下同步是空操作 */
    val cloudAvailable: Boolean
}
