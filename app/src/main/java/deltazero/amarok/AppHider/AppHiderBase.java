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

    public abstract CheckAvailabilityResult checkAvailability();

    public abstract void active(OnActivateCallbackListener onActivateCallbackListener);

    public abstract String getName();

    public static class CheckAvailabilityResult {
        public enum Result {AVAILABLE, REQ_PERM, UNAVAILABLE}

        public CheckAvailabilityResult(Result result) {
            this.result = result;
            this.msgResID = 0; // ID_NULL
        }

        public CheckAvailabilityResult(Result result, int msgResID) {
            this.result = result;
            this.msgResID = msgResID;
        }

        public Result result;
        public int msgResID;
    }

    public interface OnActivateCallbackListener {
        void onActivateCallback(Class<? extends AppHiderBase> appHider, boolean success, int msgResID);
    }
}