package com.mulin.us.data.repository.impl

import com.mulin.us.data.local.YuniDatabase
import com.mulin.us.data.local.entity.DailyQuestionEntity
import com.mulin.us.data.local.toModel
import com.mulin.us.data.remote.RemoteResult
import com.mulin.us.data.remote.cloudbase.CloudSessionState
import com.mulin.us.data.remote.ecs.EcsGate
import com.mulin.us.domain.model.Comment
import com.mulin.us.domain.model.DailyQuestion
import com.mulin.us.domain.model.Message
import com.mulin.us.domain.repository.CommentRepository
import com.mulin.us.domain.repository.MessageRepository
import com.mulin.us.domain.repository.QuestionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

/**
 * S5 问答与悄悄话的 ECS 实现。
 *
 * 只走 EcsGate（统一鉴权 + coupleId 头 + 错误映射），
 * 换回 CloudBase 时实现同一组接口即可，UI 零改动。
 */
class EcsQuestionRepository(
    private val gate: EcsGate,
    private val db: YuniDatabase
) : QuestionRepository {

    override suspend fun today(epochDay: Long): Result<DailyQuestion> = when (
        val r = gate.get("/question/today?epochDay=$epochDay")
    ) {
        is RemoteResult.Success -> {
            val o = r.data
            val model = DailyQuestion(
                cloudId = o.optString("id").ifBlank { null },
                questionEpochDay = epochDay,
                content = o.optString("content"),
                myAnswer = if (o.isNull("myAnswer")) null else o.optString("myAnswer").ifBlank { null },
                // 服务端保证未揭晓时不下发；这里再判一次 revealed，双保险
                partnerAnswer = if (o.optBoolean("revealed")) o.optString("partnerAnswer").ifBlank { null } else null,
                revealed = o.optBoolean("revealed")
            )
            cache(listOf(model))
            Result.success(model)
        }
        else -> Result.failure(IllegalStateException(errorText(r)))
    }

    override suspend fun answer(epochDay: Long, answer: String): Result<Boolean> = when (
        val r = gate.post("/question/answer", JSONObject().put("epochDay", epochDay).put("answer", answer))
    ) {
        is RemoteResult.Success -> {
            // 回答后可能刚好揭晓，刷新今日缓存，时间线立刻跟着变
            today(epochDay)
            Result.success(r.data.optBoolean("revealed"))
        }
        else -> Result.failure(IllegalStateException(errorText(r)))
    }

    override suspend fun history(limit: Int): Result<List<DailyQuestion>> = when (
        val r = gate.getArray("/question/history?limit=$limit")
    ) {
        is RemoteResult.Success -> {
            val list = (0 until r.data.length()).map { i ->
                val o = r.data.getJSONObject(i)
                val revealed = o.optBoolean("revealed")
                DailyQuestion(
                    cloudId = o.optString("id").ifBlank { null },
                    questionEpochDay = o.optLong("question_epoch_day"),
                    content = o.optString("content"),
                    myAnswer = o.optString("my_answer").ifBlank { null },
                    partnerAnswer = if (revealed) o.optString("partner_answer").ifBlank { null } else null,
                    revealed = revealed
                )
            }
            cache(list)
            Result.success(list)
        }
        else -> Result.failure(IllegalStateException(errorText(r)))
    }

    override fun observeRevealed(): Flow<List<DailyQuestion>> =
        db.dailyQuestionDao().observeRevealed().map { list -> list.map { it.toModel() } }

    /** 只缓存服务端明确返回的字段；未揭晓的 partnerAnswer 保持 null */
    private suspend fun cache(list: List<DailyQuestion>) {
        val coupleId = CloudSessionState.coupleId
        list.forEach { q ->
            db.dailyQuestionDao().upsert(DailyQuestionEntity(
                cloudId = q.cloudId,
                coupleId = coupleId,
                questionEpochDay = q.questionEpochDay,
                content = q.content,
                myAnswer = q.myAnswer,
                partnerAnswer = if (q.revealed) q.partnerAnswer else null,
                revealed = q.revealed,
                createdAt = q.createdAt,
                syncStatus = "SYNCED"
            ))
        }
    }

    private fun errorText(r: RemoteResult<*>) = when (r) {
        is RemoteResult.Unauthorized -> "请先登录云端账号"
        is RemoteResult.Forbidden -> "没有权限访问这个空间"
        is RemoteResult.Network -> "网络不可用：${r.message}"
        is RemoteResult.Conflict -> r.message
        is RemoteResult.BadRequest -> r.message
        is RemoteResult.Server -> r.message
        else -> "未知错误"
    }
}

class EcsMessageRepository(private val gate: EcsGate) : MessageRepository {
    override suspend fun send(content: String): Result<Message> = when (
        val r = gate.post("/message/send", JSONObject().put("content", content))
    ) {
        is RemoteResult.Success -> Result.success(Message(
            cloudId = r.data.optString("id"),
            senderId = r.data.optString("senderId"),
            content = r.data.optString("content"),
            createdAt = r.data.optLong("createdAt")
        ))
        else -> Result.failure(IllegalStateException("发送失败：${r}"))
    }

    override suspend fun list(limit: Int): Result<List<Message>> = when (
        val r = gate.getArray("/message/list?limit=$limit")
    ) {
        is RemoteResult.Success -> Result.success((0 until r.data.length()).map { i ->
            val o = r.data.getJSONObject(i)
            Message(
                cloudId = o.optString("id"),
                senderId = o.optString("senderId"),
                content = o.optString("content"),
                readAt = if (o.isNull("readAt")) null else o.optLong("readAt"),
                createdAt = o.optLong("createdAt")
            )
        })
        else -> Result.failure(IllegalStateException("加载消息失败：$r"))
    }

    override suspend fun markRead(id: String): Result<Unit> = when (
        val r = gate.post("/message/read", JSONObject().put("id", id))
    ) {
        is RemoteResult.Success -> Result.success(Unit)
        else -> Result.failure(IllegalStateException("标记已读失败：$r"))
    }

    override suspend fun unreadCount(): Result<Int> = when (
        val r = gate.get("/unread")
    ) {
        is RemoteResult.Success -> Result.success(r.data.optInt("count", 0))
        else -> Result.failure(IllegalStateException("查询未读失败：$r"))
    }
}

/** 5.6 回忆评论 ECS 实现 */
class EcsCommentRepository(private val gate: EcsGate) : CommentRepository {
    override suspend fun list(memoryCloudId: String): Result<List<Comment>> = when (
        val r = gate.getArray("/comment/list?memoryId=$memoryCloudId")
    ) {
        is RemoteResult.Success -> Result.success((0 until r.data.length()).map { i ->
            val o = r.data.getJSONObject(i)
            Comment(
                id = o.optString("id"),
                memoryCloudId = o.optString("memoryId"),
                authorId = o.optString("authorId"),
                content = o.optString("content"),
                mine = o.optBoolean("mine"),
                createdAt = o.optLong("createdAt")
            )
        })
        else -> Result.failure(IllegalStateException("加载评论失败：$r"))
    }

    override suspend fun add(memoryCloudId: String, content: String): Result<Comment> = when (
        val r = gate.post("/comment/add", JSONObject().put("memoryId", memoryCloudId).put("content", content))
    ) {
        is RemoteResult.Success -> Result.success(Comment(
            id = r.data.optString("id"),
            memoryCloudId = r.data.optString("memoryId"),
            authorId = r.data.optString("authorId"),
            content = r.data.optString("content"),
            createdAt = r.data.optLong("createdAt")
        ))
        else -> Result.failure(IllegalStateException("发表评论失败：$r"))
    }
}
