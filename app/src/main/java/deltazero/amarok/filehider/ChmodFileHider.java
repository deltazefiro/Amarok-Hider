package deltazero.amarok.filehider;

import android.content.Context;
import android.util.Log;

import com.topjohnwu.superuser.Shell;

import java.util.HashSet;
import java.util.Set;

import deltazero.amarok.R;
import deltazero.amarok.utils.MediaStoreHelper;

public class ChmodFileHider extends BaseFileHider {

    public ChmodFileHider(Context context) {
        super(context);
    }

    @Override
    protected void process(Set<String> targetDirs, ProcessMethod method) throws InterruptedException {
        var processDirs = new HashSet<String>();
        for (var d : targetDirs) {
            if (d.startsWith("/storage/emulated/0/"))
                processDirs.add(d.replace("/storage/emulated/", "/data/media/"));
            else Log.w("ChmodFileHider", String.format("Unsupported path: %s", d));
        }

        Shell.Job processJob = Shell.getShell().newJob();

        processDirs.stream()
                .map(d -> String.format("chmod -R %s %s", method == ProcessMethod.HIDE ? 0 : 2770, d))
                .forEach(processJob::add);

        processJob.submit(result -> MediaStoreHelper.scan(context, processDirs));
    }

    @Override
    public void tryToActive(ActivationCallbackListener activationCallbackListener) {
        Shell.getShell(shell -> {
            if (shell.isRoot()) {
                activationCallbackListener.onActivateCallback(ChmodFileHider.this.getClass(), true, 0);
            } else {
                activationCallbackListener.onActivateCallback(ChmodFileHider.this.getClass(), false, R.string.root_not_ava);
            }
        });
    }

    @Override
    public String getName() {
        return "Chmod";
    }
}
