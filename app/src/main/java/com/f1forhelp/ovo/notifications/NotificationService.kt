package com.f1forhelp.ovo.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.f1forhelp.ovo.AppManager
import com.f1forhelp.ovo.NotificationObject
import com.f1forhelp.ovo.NotificationType
import com.f1forhelp.ovo.SettingsStore.NotificationSettings
import com.f1forhelp.ovo.data.Cycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object NotificationService {

    @SuppressLint("MissingPermission")
    fun scheduleAllNotifications(context: Context) { // Used when notifications are enabled or when "Reset Scheduled Notifications button is pressed.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            AppManager.instance.popupMessage(context,"Notifications permission denied! \nCannot schedule notifications.")
            return
        }

        clearAllNotifications(context)

        val cycle = Cycle.getMostRecent()
        val toSchedule: MutableSet<String> = mutableSetOf() // Gets converted to regular set

        NotificationSettings.notifications.forEach {
            scheduleCycleNotification(context, it, cycle)
            toSchedule.add(it.name)
        }

        NotificationSettings.updateScheduledNotifications(toSchedule.toSet())

        // Double check!!
        checkScheduledNotifications(context, NotificationSettings.scheduledNotifications, true)
    }

    fun clearAllNotifications(context: Context) {
        NotificationSettings.scheduledNotifications.forEach {
            WorkManager.getInstance(context).cancelUniqueWork(it)
        }
        NotificationSettings.updateScheduledNotifications(emptySet())

        // Double check!!
        checkScheduledNotifications(context, NotificationSettings.scheduledNotifications)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun scheduleCycleNotification(context: Context, notificationObject: NotificationObject, cycle: Cycle) {
        val nextStartMs = cycle.predictedNextStartMs
        val notifyTime = when (notificationObject.type) {
            NotificationType.DAY_OF -> nextStartMs
            NotificationType.MAD ->  nextStartMs - (notificationObject.value * cycle.madLength).toLong()
            NotificationType.DAYS -> nextStartMs - (notificationObject.value * 24 * 60 * 60 * 1000).toLong()
            NotificationType.OVO_DAY -> cycle.predictedNextOvulationMs
        }
        val now = System.currentTimeMillis()
        val delay: Long = notifyTime - now

        Log.d("notifications", "delay: $delay")
        Log.d("notifications", "now: $now")
        Log.d("notifications", "cycle.predictionDateMs: $nextStartMs")

        if (delay <= 0) {
            showNotification(context, notificationObject.name, notificationObject.notificationMessage())
            return
        }

        val request = OneTimeWorkRequestBuilder<CycleNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("uniqueName" to notificationObject.name, "message" to notificationObject.notificationMessage()))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            //"cycle_notification",
            notificationObject.name,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showNotification(context: Context, uniqueName: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(uniqueName)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        // Use hashCode of unique name to get a unique integer ID
        val notifId = uniqueName.hashCode()
        NotificationManagerCompat.from(context).notify(notifId, notification)
    }

    //region Init
    private const val CHANNEL_ID = "cycle_channel"

    fun init(context: Context) {
        createNotificationChannel(context)

        // Double check!!
        checkScheduledNotifications(context, NotificationSettings.scheduledNotifications)
    }

    fun createNotificationChannel(context: Context) {
        val name = "Cycle Notifications"
        val descriptionText = "Notifications about predicted cycles"

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel(CHANNEL_ID) // remove old one

        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            enableVibration(true)
            vibrationPattern = longArrayOf(
                0, 50,
                50, 50,
                50, 50,
                50, 50,
                50, 50,
                50, 50,
                50, 50,
                50, 50
            )
        }

        val notificationManager: NotificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun checkScheduledNotifications(context: Context, notificationIds: Set<String>, givePopup: Boolean = false) {
        Log.d("notifications", "NotificationIds: $notificationIds.toString()")

        // Launch a coroutine on the IO dispatcher (background thread)
        CoroutineScope(Dispatchers.IO).launch {
            val scheduled = mutableListOf<String>()
            val workManager = WorkManager.getInstance(context)

            for (id in notificationIds) {
                try {
                    val infos = workManager.getWorkInfosForUniqueWork(id).get() // blocking call OK on IO thread
                    if (infos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }) {
                        scheduled.add(id)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Log results or update your app state
            Log.d("Startup", "Scheduled notifications on startup: $scheduled")
            if (givePopup) {
                // Switch to Main thread for UI
                withContext(Dispatchers.Main) {
                    AppManager.instance.popupMessage(context,"Scheduled Notifications: " + scheduled.count().toString())
                }
            }
        }
    }
    //endregion
}
class CycleNotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun doWork(): Result {
        val uniqueName = inputData.getString("uniqueName") ?: "Cycle Reminder"
        val message = inputData.getString("message") ?: "Your predicted cycle is approaching."
        NotificationService.showNotification(applicationContext, uniqueName, message)
        return Result.success()
    }
}