package com.mulin.us.data.reminder

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 本地提醒调度（S5.9）。
 *
 * 每天固定时间发一条本地通知，不联网。
 * 用 WorkManager 而不是 AlarmManager：省电、重启后仍在、与现有同步 Worker 同一套机制。
 */
class ReminderScheduler(private val context: Context) {

    fun scheduleDaily(hour: Int, minute: Int, text: String = "今天有想记录的吗？") {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        if (next.before(now)) next.add(Calendar.DAY_OF_YEAR, 1)
        val delay = next.timeInMillis - now.timeInMillis

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder()
                .putString(ReminderWorker.KEY_TITLE, "予你")
                .putString(ReminderWorker.KEY_TEXT, text)
                .build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME, ExistingPeriodicWorkPolicy.REPLACE, request
        )
    }

    fun cancel() = WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)

    companion object { const val WORK_NAME = "yuni_daily_reminder" }
}
