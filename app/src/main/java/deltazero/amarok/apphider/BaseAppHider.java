package deltazero.amarok.apphider;

import android.content.Context;

import java.util.Set;

public abstract class BaseAppHider {
    public Context context;

    public BaseAppHider(Context context) {
        this.context = context;
    }

    /**
     * Hide apps with option to only disable them (skip the hide step)
     * @param pkgNames Package names to hide
     * @param disableOnly If true, only disable apps without hiding them from system
     */
    public abstract void hide(Set<String> pkgNames, boolean disableOnly);

    public abstract void unhide(Set<String> pkgNames);

    public abstract void tryToActivate(ActivationCallbackListener activationCallbackListener);

    public abstract String getName();

    public interface ActivationCallbackListener {
        void onActivateCallback(Class<? extends BaseAppHider> appHider, boolean success, int msgResID);
    }
}