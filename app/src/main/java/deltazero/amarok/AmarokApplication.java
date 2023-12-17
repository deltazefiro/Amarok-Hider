package deltazero.amarok;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;

import com.google.android.material.color.DynamicColors;
import com.rosan.dhizuku.api.Dhizuku;

import deltazero.amarok.receivers.ScreenLockReceiver;
import deltazero.amarok.utils.AppCenterUtil;

public class AmarokApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // WARNING: Do not change the order of those initializations.
        PrefMgr.init(this);
        Hider.init();
        QSTileService.init(getApplicationContext());

        if (PrefMgr.getEnableDynamicColor())
            DynamicColors.applyToActivitiesIfAvailable(this);

        registerReceiver(new ScreenLockReceiver(), new IntentFilter(Intent.ACTION_SCREEN_OFF));

        // Start PanicButton service
        QuickHideService.startService(this);

        // Start App-center
        AppCenterUtil.startAppCenter(this);

        // init Dhizuku
        Dhizuku.init();
    }
}
