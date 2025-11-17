package deltazero.amarok.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import deltazero.amarok.Hider;
import deltazero.amarok.R;

public class ToggleWidget extends AppWidgetProvider {
    private static final String TAG = "ToggleWidget";
    private static final String ACTION_TOGGLE = "deltazero.amarok.widget.ACTION_TOGGLE";
    public static boolean initialized = false;

    /**
     * Initialize widget state observer. Should be invoked in {@link deltazero.amarok.AmarokApplication#onCreate()},
     * after {@link Hider#state} is initialized.
     *
     * @param context Application context
     */
    public static void init(Context context) {
        assert Hider.initialized;
        Hider.state.observeForever(state -> {
            Log.i(TAG, "State changed, updating all widgets.");
            updateAllWidgets(context);
        });
        initialized = true;
    }

    private static void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, ToggleWidget.class));
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_TOGGLE.equals(intent.getAction())) {
            // Toggle hide/unhide state
            if (Hider.getState() == Hider.State.HIDDEN) {
                Hider.unhide(context);
            } else if (Hider.getState() == Hider.State.VISIBLE) {
                Hider.hide(context);
            }
            // Widget will be automatically updated via state observer
        }
    }

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_toggle);

        Hider.State state = Hider.getState();
        boolean isHidden = (state == Hider.State.HIDDEN);
        boolean isProcessing = (state == Hider.State.PROCESSING);

        // Show/hide progress spinner
        if (isProcessing) {
            views.setViewVisibility(R.id.widget_toggle_progress, android.view.View.VISIBLE);
            views.setViewVisibility(R.id.widget_toggle_button, android.view.View.INVISIBLE);
        } else {
            views.setViewVisibility(R.id.widget_toggle_progress, android.view.View.GONE);
            views.setViewVisibility(R.id.widget_toggle_button, android.view.View.VISIBLE);

            // Set the icon and background based on current state
            int iconResource = isHidden
                    ? R.drawable.dark_mode_24dp_ffffff_fill0_wght400_grad0_opsz24
                    : R.drawable.brightness_empty_24dp_1f1f1f_fill0_wght400_grad0_opsz24;
            views.setImageViewResource(R.id.widget_toggle_button, iconResource);

            int backgroundResource = isHidden
                    ? R.drawable.widget_fab_background_black
                    : R.drawable.widget_fab_background_white;
            views.setInt(R.id.widget_toggle_button, "setBackgroundResource", backgroundResource);
        }

        // Create pending intent for widget click
        Intent intent = new Intent(context, ToggleWidget.class);
        intent.setAction(ACTION_TOGGLE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_toggle_button, pendingIntent);

        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
} 