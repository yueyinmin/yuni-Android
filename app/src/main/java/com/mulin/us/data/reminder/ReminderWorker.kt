package com.mulin.us.data.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * 本地提醒 Worker（S5.9）。
 *
 * 不上架、拿不到厂商推送通道，所以只用系统本地通知：
 * 定时触发 → 发一条"今天有想记录的吗"——不联网、不耗云端资源点。
 */
class ReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: "予你"
        val text = inputData.getString(KEY_TEXT) ?: "今天有想记录的吗？"
        showNotification(title, text)
        return Result.success()
    }

    private fun showNotification(title: String, text: String) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "予你提醒", NotificationManager.IMPORTANCE_DEFAULT)
            nm.createNotificationChannel(channel)
        }
        val n = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission")
        nm.notify(NOTIFY_ID, n)
    }

    companion object {
        const val CHANNEL_ID = "yuni_reminder"
        const val NOTIFY_ID = 1001
        const val KEY_TITLE = "title"
        const val KEY_TEXT = "text"
    }
}
