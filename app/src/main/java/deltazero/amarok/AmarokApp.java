package deltazero.amarok;

import android.app.Application;

public class AmarokApp extends Application {

    public PanicButton panicButton;

    @Override
    public void onCreate() {
        super.onCreate();

        // Start PanicButton
        panicButton = new PanicButton(this);
    }
}
