package pm.kajdan.everysingleminute

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
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

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }

        setupMinuteUpdateAlarm(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelMinuteUpdateAlarm(context)
    }

    private fun setupMinuteUpdateAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val calendar = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, 1)
        }

        val updateIntent = Intent(context, TimeWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, getAppWidgetIds(context))
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, updateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }

    private fun cancelMinuteUpdateAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, TimeWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        val clockIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val clockPendingIntent = PendingIntent.getActivity(
            context,
            0,
            clockIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        views.setOnClickPendingIntent(
            R.id.widget_container,
            clockPendingIntent
        )

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTime = timeFormat.format(Date())

        val entry = timeEntries.find { it.hour == currentTime }

        if (entry != null) {
            views.setTextViewText(R.id.text_quote, formatQuote(entry.timeToBold, entry.quote))
            views.setTextViewText(
                R.id.text_title_author,
                formatTitleAuthor(context, entry.author, entry.book)
            )
        } else {
            views.setTextViewText(R.id.text_quote, "No entry found for this minute.")
            views.setTextViewText(R.id.text_title_author, ":(")
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }


    private fun readCsv(context: Context, resourceId: Int): List<TimeEntry> {
        val timeEntries = mutableListOf<TimeEntry>()
        val inputStream = context.resources.openRawResource(resourceId)
        val reader = BufferedReader(InputStreamReader(inputStream))

        reader.readLine()

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val nextLine = line!!.split('|')
            if (nextLine.size >= 5) {
                timeEntries.add(
                    TimeEntry(
                        nextLine[0], nextLine[1], nextLine[2], nextLine[3], nextLine[4]
                    )
                )
            }
        }
        reader.close()

        return timeEntries
    }

    private fun formatQuote(boldWord: String, text: String): CharSequence {
        val spannableString = SpannableString(text)
        val startIndex = text.lowercase().indexOf(boldWord.lowercase())
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

    private fun formatTitleAuthor(context: Context, author: String, book: String): CharSequence {
        val quoteOpen = context.getString(R.string.quote_open)
        val quoteClose = context.getString(R.string.quote_close)

        val authorWithNbsp = ("— $author").replace(" ", "\u00A0")
        val formattedText = "$authorWithNbsp, $quoteOpen$book$quoteClose"
        val spannableString = SpannableString(formattedText)

        val start = formattedText.indexOf(book) - 1 // 1 for the leading quote
        val end = start + book.length + 2 // 2 for the quotes
        spannableString.setSpan(
            StyleSpan(android.graphics.Typeface.ITALIC),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return spannableString
    }

    private fun getAppWidgetIds(context: Context): IntArray {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        return appWidgetManager.getAppWidgetIds(ComponentName(context, TimeWidget::class.java))
    }
}
