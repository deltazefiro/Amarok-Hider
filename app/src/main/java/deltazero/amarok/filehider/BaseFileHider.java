package deltazero.amarok.filehider;

import android.content.Context;

import java.util.Set;

public abstract class BaseFileHider {

    protected final Context context;

    public BaseFileHider(Context context) {
        this.context = context;
    }

    protected abstract void process(Set<String> targetDirs, ProcessMethod method) throws InterruptedException;

    public abstract void tryToActive(ActivationCallbackListener activationCallbackListener);

    public abstract String getName();

    protected enum ProcessMethod {
        HIDE, UNHIDE
    }

    public void hide(Set<String> targetDirs) throws InterruptedException {
        process(targetDirs, ProcessMethod.HIDE);
    }

    public void unhide(Set<String> targetDirs) throws InterruptedException {
        process(targetDirs, ProcessMethod.UNHIDE);
    }

    public interface ActivationCallbackListener {
        void onActivateCallback(Class<? extends BaseFileHider> appHider, boolean success, int msgResID);
    }
}
