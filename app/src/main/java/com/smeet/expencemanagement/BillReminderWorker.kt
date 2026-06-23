package com.smeet.expencemanagement

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.concurrent.TimeUnit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smeet.expencemanagement.model.ExpenseDatabase
import java.util.Calendar

class BillReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
): CoroutineWorker(context,workerParams) {
    override suspend fun doWork(): Result {
        val database= ExpenseDatabase.getDatabase(context)
        val scheduledBillDao=database.ScheduledBillDao()

        return try {
            // 1. Fetch the data snapshot
            val allBills = scheduledBillDao.getAllBillSync()

            // 2. Set up "Today's" timeline
            val today = System.currentTimeMillis()
            val calCurrent = Calendar.getInstance().apply {
                timeInMillis = today
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(
                Calendar.MILLISECOND,
                0
            )
            }

            val upcomingBills = allBills.filter { !it.isPaid }.filter { bill ->
                val calDue = Calendar.getInstance().apply {
                    timeInMillis = bill.dueDate
                    set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE, 0); set(
                    Calendar.SECOND,
                    0
                ); set(Calendar.MILLISECOND, 0)
                }
                val daysDifference =TimeUnit.MILLISECONDS.toDays(calDue.timeInMillis - calCurrent.timeInMillis)

                daysDifference <= 0L
            }
            if (upcomingBills.isNotEmpty()) {
                sendNotification(upcomingBills.size)
            }
            Result.success()
        }catch (e: Exception){
            Result.failure()
        }
    }

    private fun sendNotification(billCount:Int){
        val channelID="bill_reminders"
        val notificationManager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelID, "Bill Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val intent= Intent(context, Reminder::class.java).apply {
            flags= Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent= PendingIntent.getActivity(context,0,intent, PendingIntent.FLAG_IMMUTABLE)

        val notification= NotificationCompat.Builder(context,channelID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("Upcoming Bills")
            .setContentText("You have \$billCount unpaid bill(s) due soon or overdue.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1,notification)
    }
}