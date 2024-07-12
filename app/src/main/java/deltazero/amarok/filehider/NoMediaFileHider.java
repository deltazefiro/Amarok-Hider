package deltazero.amarok.filehider;

import android.content.Context;
import android.util.Log;

import java.nio.file.Paths;
import java.util.Set;

import deltazero.amarok.utils.MediaStoreHelper;

public class NoMediaFileHider extends BaseFileHider {

    private static final String TAG = "NoMediaFileHider";

    public NoMediaFileHider(Context context) {
        super(context);
    }

    @Override
    protected void process(Set<String> targetDirs, ProcessMethod method) throws InterruptedException {
        for (var dir : targetDirs) {

            if (Thread.interrupted())
                throw new InterruptedException();

            Log.i(TAG, String.format("Processing: %s", dir));

            try {

                var path = Paths.get(dir);
                var nomediaFile = path.resolve(".nomedia").toFile();

                if (method == ProcessMethod.HIDE) {

                    var result = nomediaFile.createNewFile();
                    if (!result) Log.w(TAG, String.format(".nomedia already exist: %s", path));

                } else if (method == ProcessMethod.UNHIDE) {

                    if (!nomediaFile.isFile()) {
                        Log.w(TAG, String.format(".nomedia not file: %s", path));
                        continue;
                    }

                    var result = nomediaFile.delete();
                    if (!result) Log.w(TAG, String.format("Failed to remove .nomedia: %s", path));
                }

            } catch (Exception e) {
                Log.w(TAG, String.format("Error while processing %s: ", dir), e);
            }
        }

        MediaStoreHelper.scan(context, targetDirs);
    }

    @Override
    public void tryToActive(ActivationCallbackListener activationCallbackListener) {
        activationCallbackListener.onActivateCallback(this.getClass(), true, 0);
    }

    @Override
    public String getName() {
        return "NoMedia";
    }
}
