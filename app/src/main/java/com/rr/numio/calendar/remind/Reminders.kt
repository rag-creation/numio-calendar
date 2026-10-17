package com.rr.numio.calendar.remind

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rr.numio.calendar.MainActivity
import com.rr.numio.calendar.R
import com.rr.numio.calendar.data.CalendarSettings
import com.rr.numio.calendar.data.Event
import com.rr.numio.calendar.data.EventStore
import com.rr.numio.calendar.data.Recurrence
import com.rr.numio.calendar.hijri.Hijri
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.concurrent.thread

/**
 * Event reminders. Only the next reminder is ever set with Android's alarm clock; when it rings,
 * the notification is shown and the one after it is set. Everything stays on the phone.
 */
object Reminders {
    private const val CHANNEL = "reminders"
    private const val ACTION_FIRE = "com.rr.numio.calendar.REMIND"
    private const val EXTRA_TITLES = "titles"
    private const val EXTRA_TEXTS = "texts"

    /** When the reminder for one occurrence of [e] goes off. */
    fun triggerAt(e: Event): LocalDateTime =
        e.date.atTime(e.start ?: LocalTime.of(9, 0)).minusMinutes(e.reminder.toLong())

    /** Work out the next reminder and set the alarm for it (in the background). */
    fun schedule(context: Context, done: (() -> Unit)? = null) {
        val app = context.applicationContext
        thread(name = "reminders") {
            try { scheduleNow(app) } finally { done?.invoke() }
        }
    }

    private fun scheduleNow(context: Context) {
        val settings = CalendarSettings.load(context)
        val withReminder = EventStore.load(context).filter { it.reminder >= 0 }
        val am = context.getSystemService(AlarmManager::class.java)
        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val upcoming = Recurrence.expand(withReminder, today, today.plusDays(400)) { d ->
            Hijri.of(d, settings.hijriAdjust).let { it.month to it.day }
        }.map { it to triggerAt(it) }.filter { it.second.isAfter(now) }

        val next = upcoming.minOfOrNull { it.second }
        val pending = pendingIntent(context, upcoming.filter { it.second == next }.map { it.first })
        if (next == null) { am.cancel(pending); return }

        val at = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
    }

    private fun pendingIntent(context: Context, due: List<Event>): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE)
            .putExtra(EXTRA_TITLES, due.map { it.title }.toTypedArray())
            .putExtra(EXTRA_TEXTS, due.map { describe(it) }.toTypedArray())
        return PendingIntent.getBroadcast(
            context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun describe(e: Event): String {
        val day = when (e.date) {
            LocalDate.now() -> "Today"
            LocalDate.now().plusDays(1) -> "Tomorrow"
            else -> e.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        }
        val time = e.start?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)) ?: "all day"
        return "$day · $time"
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    internal fun fire(context: Context, intent: Intent) {
        val titles = intent.getStringArrayExtra(EXTRA_TITLES) ?: return
        val texts = intent.getStringArrayExtra(EXTRA_TEXTS) ?: return
        if (!canNotify(context)) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Reminders", NotificationManager.IMPORTANCE_HIGH))
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        titles.forEachIndexed { i, title ->
            val n = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(texts.getOrNull(i))
                .setContentIntent(open)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build()
            try {
                NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % 100000).toInt() + i, n)
            } catch (e: SecurityException) { /* notifications switched off */ }
        }
    }

    const val FIRE = ACTION_FIRE
}

/** Shows due reminders and sets the next one; also re-sets it after a restart or a clock change. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Reminders.FIRE) Reminders.fire(context, intent)
        val result = goAsync()
        Reminders.schedule(context) { result.finish() }
    }
}
