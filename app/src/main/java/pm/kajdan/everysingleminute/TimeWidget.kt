package pm.kajdan.everysingleminute

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.widget.RemoteViews
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

class TimeWidget : AppWidgetProvider() {

    private lateinit var timeEntries: List<TimeEntry>

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (!::timeEntries.isInitialized) {
            timeEntries = readCsv(context, R.raw.time)
        }

        // Update each widget
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }

        // Set up the AlarmManager to update the widget every minute
        setupMinuteUpdateAlarm(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Cancel the alarm when the last widget is disabled
        cancelMinuteUpdateAlarm(context)
    }

    private fun setupMinuteUpdateAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Intent to update the widget
        val intent = Intent(context, TimeWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }

        // PendingIntent for the alarm
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule the exact alarm to trigger every minute
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 60000, // First trigger in 1 minute
            pendingIntent
        )
    }

    private fun cancelMinuteUpdateAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancel the PendingIntent used to update the widget
        val intent = Intent(context, TimeWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel the alarm
        alarmManager.cancel(pendingIntent)
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        // Get the current time
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTime = timeFormat.format(Date())

        // Find the corresponding entry for the current time
        val entry = timeEntries.find { it.hour == currentTime }

        if (entry != null) {
            views.setTextViewText(R.id.text_quote, formatText(entry.timeToBold, entry.quote))
            views.setTextViewText(R.id.text_title_author, "${entry.book} —\u00A0${entry.author}")
        } else {
            views.setTextViewText(R.id.text_quote, "No entry found for this minute.")
            views.setTextViewText(R.id.text_title_author, "")
        }

        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun readCsv(context: Context, resourceId: Int): List<TimeEntry> {
        val timeEntries = mutableListOf<TimeEntry>()
        val inputStream = context.resources.openRawResource(resourceId)
        val reader = BufferedReader(InputStreamReader(inputStream))

        // Skip the header
        reader.readLine()

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val nextLine = line!!.split('|')
            if (nextLine.size >= 5) {
                timeEntries.add(
                    TimeEntry(
                        nextLine[0],
                        nextLine[1],
                        nextLine[2],
                        nextLine[3],
                        nextLine[4]
                    )
                )
            }
        }
        reader.close()

        return timeEntries
    }

    private fun formatText(boldWord: String, text: String): CharSequence {
        val spannableString = SpannableString(text)
        val startIndex = text.indexOf(boldWord)
        if (startIndex >= 0) {
            spannableString.setSpan(
                StyleSpan(android.graphics.Typeface.BOLD),
                startIndex,
                startIndex + boldWord.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannableString
    }
}
