package protect.budgetwatch

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import protect.budgetwatch.DBHelper

class TransactionExpenseWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val widget = ComponentName(context, TransactionExpenseWidget::class.java)
        val remoteView = RemoteViews(context.packageName, R.layout.widget_layout)
        val intent = Intent(context, TransactionViewActivity::class.java)
        val extras = Bundle()
        extras.putInt("type", DBHelper.TransactionDbIds.EXPENSE)
        intent.putExtras(extras)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
        remoteView.setOnClickPendingIntent(R.id.addTransaction, pendingIntent)
        appWidgetManager.updateAppWidget(widget, remoteView)
    }
}