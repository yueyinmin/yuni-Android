package com.mulin.us.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mulin.us.data.local.dao.CoupleDao
import com.mulin.us.data.local.dao.DailyQuestionDao
import com.mulin.us.data.local.dao.DiaryDao
import com.mulin.us.data.local.dao.LetterDao
import com.mulin.us.data.local.dao.MeetingPlanDao
import com.mulin.us.data.local.dao.MemoryDao
import com.mulin.us.data.local.dao.MemoryPhotoDao
import com.mulin.us.data.local.dao.SyncCursorDao
import com.mulin.us.data.local.dao.SyncTaskDao
import com.mulin.us.data.local.dao.UserDao
import com.mulin.us.data.local.dao.WishDao
import com.mulin.us.data.local.entity.CoupleEntity
import com.mulin.us.data.local.entity.DailyQuestionEntity
import com.mulin.us.data.local.entity.DiaryEntity
import com.mulin.us.data.local.entity.LetterEntity
import com.mulin.us.data.local.entity.LocalUserEntity
import com.mulin.us.data.local.entity.MeetingPlanEntity
import com.mulin.us.data.local.entity.MemoryEntity
import com.mulin.us.data.local.entity.MemoryPhotoEntity
import com.mulin.us.data.local.entity.SyncCursorEntity
import com.mulin.us.data.local.entity.SyncTaskEntity
import com.mulin.us.data.local.entity.WishEntity

/**
 * 本地数据库。
 *
 * 名字用 yuni.db —— 和轻记的 qingji.db 完全不同，两个 App 装在一台手机上也不会互相影响。
 *
 * 关于迁移：B0 是第 1 版，还没有迁移历史。
 * 从第 2 版开始，每次改表必须写 Migration，禁止用 fallbackToDestructiveMigration()
 * ——那等于"升级一次清一次数据"，对日记类 App 是不可接受的。
 */
@Database(
    entities = [
        LocalUserEntity::class,
        CoupleEntity::class,
        MemoryEntity::class,
        MemoryPhotoEntity::class,
        DiaryEntity::class,
        DailyQuestionEntity::class,
        LetterEntity::class,
        WishEntity::class,
        MeetingPlanEntity::class,
        SyncTaskEntity::class,
        SyncCursorEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class YuniDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun coupleDao(): CoupleDao
    abstract fun memoryDao(): MemoryDao
    abstract fun memoryPhotoDao(): MemoryPhotoDao
    abstract fun diaryDao(): DiaryDao
    abstract fun dailyQuestionDao(): DailyQuestionDao
    abstract fun letterDao(): LetterDao
    abstract fun wishDao(): WishDao
    abstract fun meetingPlanDao(): MeetingPlanDao
    abstract fun syncTaskDao(): SyncTaskDao
    abstract fun syncCursorDao(): SyncCursorDao

    companion object {
        private const val DB_NAME = "yuni.db"

        @Volatile
        private var instance: YuniDatabase? = null

        fun get(context: Context): YuniDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    YuniDatabase::class.java,
                    DB_NAME
                ).build().also { instance = it }
            }
    }
}
