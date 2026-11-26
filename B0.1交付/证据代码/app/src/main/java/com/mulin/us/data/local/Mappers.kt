package com.mulin.us.data.local

import com.mulin.us.data.local.entity.CoupleEntity
import com.mulin.us.data.local.entity.DailyQuestionEntity
import com.mulin.us.data.local.entity.DiaryEntity
import com.mulin.us.data.local.entity.LetterEntity
import com.mulin.us.data.local.entity.LocalUserEntity
import com.mulin.us.data.local.entity.MeetingPlanEntity
import com.mulin.us.data.local.entity.MemoryEntity
import com.mulin.us.data.local.entity.MessageEntity
import com.mulin.us.data.local.entity.WishEntity
import com.mulin.us.domain.model.CoupleProfile
import com.mulin.us.domain.model.DailyQuestion
import com.mulin.us.domain.model.Diary
import com.mulin.us.domain.model.DiaryVisibility
import com.mulin.us.domain.model.Letter
import com.mulin.us.domain.model.LetterStatus
import com.mulin.us.domain.model.LocalUser
import com.mulin.us.domain.model.MeetingPlan
import com.mulin.us.domain.model.Memory
import com.mulin.us.domain.model.Message
import com.mulin.us.domain.model.SyncStatus
import com.mulin.us.domain.model.Wish

/**
 * 实体 ↔ 模型 的转换。
 *
 * 集中放一处，避免每个 Repository 各写一遍（写两遍必有一天不一致）。
 */

fun LocalUserEntity.toModel() = LocalUser(
    id = id,
    username = username,
    nickname = nickname,
    avatarLocalPath = avatarLocalPath,
    createdAt = createdAt
)

fun LocalUser.toEntity() = LocalUserEntity(
    id = id,
    username = username,
    nickname = nickname,
    avatarLocalPath = avatarLocalPath,
    createdAt = createdAt
)

fun CoupleEntity.toModel() = CoupleProfile(
    id = id,
    memberAId = memberAId,
    memberBId = memberBId,
    relationshipStartEpochDay = relationshipStartEpochDay,
    status = status,
    createdAt = createdAt
)

fun CoupleProfile.toEntity() = CoupleEntity(
    id = id,
    memberAId = memberAId,
    memberBId = memberBId,
    relationshipStartEpochDay = relationshipStartEpochDay,
    status = status,
    createdAt = createdAt
)

fun MemoryEntity.toModel() = Memory(
    localId = localId,
    cloudId = cloudId,
    title = title,
    content = content,
    happenedAt = happenedAt,
    mood = mood,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = SyncStatus.from(syncStatus)
)

fun DiaryEntity.toModel() = Diary(
    localId = localId,
    cloudId = cloudId,
    coupleId = coupleId,
    ownerId = ownerId,
    visibility = DiaryVisibility.from(visibility),
    title = title,
    content = content,
    entryEpochDay = entryEpochDay,
    mood = mood,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = SyncStatus.from(syncStatus)
)

fun WishEntity.toModel() = Wish(
    localId = localId,
    cloudId = cloudId,
    title = title,
    note = note,
    targetAt = targetAt,
    doneAt = doneAt,
    priority = priority,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = SyncStatus.from(syncStatus)
)

fun MeetingPlanEntity.toModel() = MeetingPlan(
    localId = localId,
    cloudId = cloudId,
    plannedAt = plannedAt,
    place = place,
    note = note,
    doneAt = doneAt,
    createdAt = createdAt,
    deletedAt = deletedAt,
    syncStatus = SyncStatus.from(syncStatus)
)

fun DailyQuestionEntity.toModel() = DailyQuestion(
    localId = localId,
    cloudId = cloudId,
    coupleId = coupleId,
    questionEpochDay = questionEpochDay,
    content = content,
    myAnswer = myAnswer,
    partnerAnswer = partnerAnswer,
    revealed = revealed,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.from(syncStatus),
    serverVersion = serverVersion
)

fun LetterEntity.toModel() = Letter(
    localId = localId,
    cloudId = cloudId,
    coupleId = coupleId,
    title = title,
    unlockAt = unlockAt,
    senderId = senderId,
    receiverId = receiverId,
    localDraftContent = localDraftContent,
    status = LetterStatus.from(status),
    hasLocalDraft = hasLocalDraft,
    createdAt = createdAt
)

fun MessageEntity.toModel() = Message(
    localId = localId,
    cloudId = cloudId,
    coupleId = coupleId,
    senderId = senderId,
    receiverId = receiverId,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
    readAt = readAt,
    deletedAt = deletedAt,
    syncStatus = SyncStatus.from(syncStatus),
    serverVersion = serverVersion
)
