package deltazero.amarok.FileHider;

import android.content.Context;

import java.util.Set;

public abstract class BaseFileHider {

    private final Context context;

    public BaseFileHider(Context context) {
        this.context = context;
    }

    protected abstract void process(Set<String> targetDirs, ProcessMethod method) throws InterruptedException;

    protected enum ProcessMethod {
        HIDE, UNHIDE
    }

    public void hide(Set<String> targetDirs) throws InterruptedException {
        process(targetDirs, ProcessMethod.HIDE);
    }

    public void unhide(Set<String> targetDirs) throws InterruptedException {
        process(targetDirs, ProcessMethod.UNHIDE);
    }
}
