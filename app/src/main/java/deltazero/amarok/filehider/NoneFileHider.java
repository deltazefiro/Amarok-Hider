package deltazero.amarok.filehider;

import android.content.Context;

import java.util.Set;

public class NoneFileHider extends BaseFileHider {

    public NoneFileHider(Context context) {
        super(context);
    }

    @Override
    protected void process(Set<String> targetDirs, ProcessMethod method) throws InterruptedException {
        return;
    }

    @Override
    public void tryToActive(ActivationCallbackListener activationCallbackListener) {
        activationCallbackListener.onActivateCallback(this.getClass(), true, 0);
    }

    @Override
    public String getName() {
        return "Disabled";
    }
}
