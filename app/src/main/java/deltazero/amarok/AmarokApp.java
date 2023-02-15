package deltazero.amarok;

import android.app.Application;

public class AmarokApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Start PanicButton service
        QuickHideService.startService(this);
    }
}
