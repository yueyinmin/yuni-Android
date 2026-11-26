package com.mulin.us.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.mulin.us.data.local.dao.CoupleDao
import com.mulin.us.data.local.dao.DailyQuestionDao
import com.mulin.us.data.local.dao.DiaryDao
import com.mulin.us.data.local.dao.LetterDao
import com.mulin.us.data.local.dao.MeetingPlanDao
import com.mulin.us.data.local.dao.MemoryDao
import com.mulin.us.data.local.dao.MemoryPhotoDao
import com.mulin.us.data.local.dao.MessageDao
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
import com.mulin.us.data.local.entity.MessageEntity
import com.mulin.us.data.local.entity.SyncCursorEntity
import com.mulin.us.data.local.entity.SyncTaskEntity
import com.mulin.us.data.local.entity.WishEntity

/**
 * 本地数据库。
 *
 * 名字用 yuni.db —— 和轻记的 qingji.db 完全不同，两个 App 装在一台手机上也不会互相影响。
 *
 * 版本历史：
 * - v1（B0）：11 张表，无迁移历史
 * - v2（B0.1 T05）：diary 加 coupleId；daily_question 加 coupleId + 3 个同步字段；
 *   letter 加 coupleId + localDraftContent + status；新增 message 表
 *
 * 从 v2 起，每次改表必须写 Migration，禁止用 fallbackToDestructiveMigration()
 * ——那等于"升级一次清一次数据"，对日记类 App 是不可接受的。
 *
 * exportSchema = true 的原因（B0.1 T05 改）：
 * 不导出 schema 就没法用 Room 官方的 MigrationTestHelper 做"真升级"测试，
 * 只能靠肉眼比对 SQL，那等于没测。导出后每版表结构都是 git 里可追溯的 json，
 * 出现"迁移后少一列"这类问题时，有据可查。
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
        MessageEntity::class,
        SyncTaskEntity::class,
        SyncCursorEntity::class
    ],
    version = 2,
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
    abstract fun messageDao(): MessageDao
    abstract fun syncTaskDao(): SyncTaskDao
    abstract fun syncCursorDao(): SyncCursorDao

    companion object {
        const val DB_NAME = "yuni.db"

        @Volatile
        private var instance: YuniDatabase? = null

        fun get(context: Context): YuniDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    YuniDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .build()
                    .also { instance = it }
            }

        /**
         * 所有迁移按顺序登记在这里。
         * 新增一版就在这里追加一条，不要在别处零散地 addMigrations ——
         * 否则"哪些迁移生效了"将无法一眼看清。
         */
        val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
    }
}
