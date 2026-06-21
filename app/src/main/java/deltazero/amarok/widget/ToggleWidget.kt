package deltazero.amarok.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import dagger.hilt.android.EntryPointAccessors
import deltazero.amarok.AmarokApplication
import deltazero.amarok.R
import deltazero.amarok.core.Hider
import deltazero.amarok.ui.SecurityAuthForQSActivity
import deltazero.amarok.utils.SecurityUtil

class ToggleWidget : AppWidgetProvider() {

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray,
  ) {
    for (appWidgetId in appWidgetIds) {
      updateAppWidget(context, appWidgetManager, appWidgetId)
    }
  }

  override fun onReceive(context: Context, intent: Intent) {
    super.onReceive(context, intent)
    if (ACTION_TOGGLE == intent.action) {
      Log.i(TAG, "Widget toggle action received.")

      if (Hider.getState() == Hider.State.PROCESSING) {
        Log.w(TAG, "Already processing. Ignoring toggle action.")
        return
      }

      if (Hider.getState() == Hider.State.HIDDEN) {
        val settingsRepo =
          EntryPointAccessors.fromApplication(
              context.applicationContext,
              AmarokApplication.RepositoryEntryPoint::class.java,
            )
            .settingsRepository()
        if (SecurityUtil.isUnlockRequired(settingsRepo)) {
          Log.i(TAG, "Security unlock required. Launching authentication activity.")
          context.startActivity(
            Intent(context, SecurityAuthForQSActivity::class.java)
              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          )
        } else {
          Hider.processAll(context, Hider.Action.UNHIDE)
        }
      } else if (Hider.getState() == Hider.State.VISIBLE) {
        Hider.processAll(context, Hider.Action.HIDE)
      }
      // Widget will be automatically updated via state observer
    }
  }

  companion object {
    private const val TAG = "ToggleWidget"
    private const val ACTION_TOGGLE = "deltazero.amarok.widget.ACTION_TOGGLE"
    @JvmField var initialized = false

    /**
     * Initialize widget state observer. Should be invoked in
     * [deltazero.amarok.AmarokApplication.onCreate], after [Hider.state] is initialized.
     *
     * @param context Application context
     */
    @JvmStatic
    fun init(context: Context) {
      assert(Hider.initialized)
      if (initialized) return

      val appContext = context.applicationContext
      Hider.stateLiveData.observeForever {
        Log.i(TAG, "State changed, updating all widgets.")
        updateAllWidgets(appContext)
      }
      initialized = true
    }

    private fun updateAllWidgets(context: Context) {
      val appWidgetManager = AppWidgetManager.getInstance(context)
      val appWidgetIds =
        appWidgetManager.getAppWidgetIds(ComponentName(context, ToggleWidget::class.java))
      for (appWidgetId in appWidgetIds) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
      }
    }

    private fun updateAppWidget(
      context: Context,
      appWidgetManager: AppWidgetManager,
      appWidgetId: Int,
    ) {
      val views = RemoteViews(context.packageName, R.layout.widget_toggle)

      val state = Hider.getState()
      val isHidden = state == Hider.State.HIDDEN
      val isProcessing = state == Hider.State.PROCESSING

      // Show/hide progress spinner
      if (isProcessing) {
        views.setViewVisibility(R.id.widget_toggle_progress, View.VISIBLE)
        views.setViewVisibility(R.id.widget_toggle_button, View.INVISIBLE)
      } else {
        views.setViewVisibility(R.id.widget_toggle_progress, View.GONE)
        views.setViewVisibility(R.id.widget_toggle_button, View.VISIBLE)

        // Set the icon and background based on current state
        val iconResource = if (isHidden) R.drawable.ic_brightness_empty else R.drawable.ic_dark_mode
        views.setImageViewResource(R.id.widget_toggle_button, iconResource)

        val backgroundResource =
          if (isHidden) R.drawable.widget_fab_background_white
          else R.drawable.widget_fab_background_black
        views.setInt(R.id.widget_toggle_button, "setBackgroundResource", backgroundResource)
      }

      // Create pending intent for widget click
      val intent = Intent(context, ToggleWidget::class.java)
      intent.action = ACTION_TOGGLE
      val pendingIntent =
        PendingIntent.getBroadcast(
          context,
          0,
          intent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
      views.setOnClickPendingIntent(R.id.widget_toggle_button, pendingIntent)

      // Update the widget
      appWidgetManager.updateAppWidget(appWidgetId, views)
    }
  }
}
