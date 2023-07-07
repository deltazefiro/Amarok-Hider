package deltazero.amarok;

import android.app.Application;

import com.rosan.dhizuku.api.Dhizuku;

public class AmarokApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Start PanicButton service
        QuickHideService.startService(this);

        // init dhizuku
        Dhizuku.init();
    }
}
