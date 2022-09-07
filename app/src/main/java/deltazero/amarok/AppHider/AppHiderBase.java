package deltazero.amarok.AppHider;

import android.content.Context;

import java.util.Set;

public abstract class AppHiderBase {
    public Context context;

    public AppHiderBase(Context context) {
        this.context = context;
    }

    public abstract void hide(Set<String> pkgNames);

    public abstract void unhide(Set<String> pkgNames);

    public abstract boolean checkAvailability();
}