package deltazero.amarok;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;

import com.google.android.material.color.DynamicColors;
import com.rosan.dhizuku.api.Dhizuku;

import deltazero.amarok.receivers.ScreenStatusReceiver;
import deltazero.amarok.utils.AppCenterUtil;
import deltazero.amarok.utils.XHidePrefBridge;
import deltazero.amarok.widget.ToggleWidget;
import jonathanfinerty.once.Once;

public class AmarokApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // WARNING: Do not change the order of those initializations.
        XHidePrefBridge.migratePrefsIfNeeded(this);
        PrefMgr.init(this);
        Hider.init();
        QSTileService.init(getApplicationContext());
        ToggleWidget.init(getApplicationContext());

        if (PrefMgr.getEnableDynamicColor())
            DynamicColors.applyToActivitiesIfAvailable(this);

        // Register ScreenStatusReceiver
        var screenStatusIntentFilter = new IntentFilter();
        screenStatusIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenStatusIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(new ScreenStatusReceiver(), screenStatusIntentFilter);

        // Initialise XHidePrefBridge
        XHidePrefBridge.init(this);

        // Start PanicButton service
        QuickHideService.startService(this);

        // Start App-center
        AppCenterUtil.startAppCenter(this);

        // init Dhizuku
        Dhizuku.init();

        // init Once
        Once.initialise(this);
    }
}
