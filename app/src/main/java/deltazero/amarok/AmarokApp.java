package deltazero.amarok;

import android.app.Application;

import com.google.android.material.color.DynamicColors;
import com.rosan.dhizuku.api.Dhizuku;

public class AmarokApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (new PrefMgr(this).getEnableDynamicColor())
            DynamicColors.applyToActivitiesIfAvailable(this);

        // Start PanicButton service
        QuickHideService.startService(this);

        // init dhizuku
        Dhizuku.init();
    }
}
