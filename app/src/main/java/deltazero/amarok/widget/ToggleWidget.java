package deltazero.amarok.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import deltazero.amarok.Hider;
import deltazero.amarok.R;

public class ToggleWidget extends AppWidgetProvider {
    private static final String ACTION_TOGGLE = "deltazero.amarok.widget.ACTION_TOGGLE";

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

            // Update all widgets
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, ToggleWidget.class));
            for (int appWidgetId : appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId);
            }
        }
    }

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_toggle);

        // Set the icon based on current state
        int iconResource = (Hider.getState() == Hider.State.HIDDEN)
                ? R.drawable.ic_paw
                : R.drawable.ic_paw;
        views.setImageViewResource(R.id.widget_toggle_button, iconResource);

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