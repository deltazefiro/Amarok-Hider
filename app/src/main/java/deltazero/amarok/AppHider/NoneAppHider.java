package deltazero.amarok.AppHider;

import android.content.Context;

import java.util.Set;

public class NoneAppHider extends AppHiderBase{
    
    // Active when no AppHider is available.

    public NoneAppHider(Context context) {
        super(context);
    }

    @Override
    public void hide(Set<String> pkgNames) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unhide(Set<String> pkgNames) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CheckAvailabilityResult checkAvailability() {
        return new CheckAvailabilityResult(CheckAvailabilityResult.Result.AVAILABLE);
    }

    @Override
    public void active(OnActivateCallbackListener onActivateCallbackListener) {
        onActivateCallbackListener.onActivateCallback(getClass(), true, 0);
    }

    @Override
    public String getName() {
        return "None";
    }
}
