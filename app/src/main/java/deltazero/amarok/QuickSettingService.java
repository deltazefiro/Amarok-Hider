package deltazero.amarok;

import android.content.Intent;
import android.os.IBinder;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ServiceLifecycleDispatcher;

public class QuickSettingService extends TileService implements LifecycleOwner {

    private static final String TAG = "TileService";

    private Tile tile;
    private Hider hider;
    private PrefMgr prefMgr;
    private MutableLiveData<Boolean> isProcessing;

    /**
     * =========================== LifecycleService =============================
     */

    private final ServiceLifecycleDispatcher mDispatcher = new ServiceLifecycleDispatcher(this);

    @CallSuper
    @Override
    public void onCreate() {
        mDispatcher.onServicePreSuperOnCreate();
        super.onCreate();
    }

    @CallSuper
    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        mDispatcher.onServicePreSuperOnBind();
        return super.onBind(intent);
    }

    @SuppressWarnings("deprecation")
    @CallSuper
    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        mDispatcher.onServicePreSuperOnStart();
        super.onStart(intent, startId);
    }

    @CallSuper
    @Override
    public void onDestroy() {
        mDispatcher.onServicePreSuperOnDestroy();
        super.onDestroy();
    }

    @Override
    @NonNull
    public Lifecycle getLifecycle() {
        return mDispatcher.getLifecycle();
    }

    /**
     * =========================== LifecycleService End =============================
     */

    private class TileUpdateObserver implements Observer<Boolean> {
        @Override
        public void onChanged(Boolean aBoolean) {
            updateTile();
        }
    }

    public void updateTile() {
        assert isProcessing.getValue() != null;

        if (isProcessing.getValue()) {
            tile.setState(Tile.STATE_UNAVAILABLE);
            tile.setLabel(getString(R.string.processing));
        } else {
            tile.setLabel(getString(R.string.app_name));
            tile.setState(prefMgr.getIsHidden() ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);
        }
        tile.updateTile();
    }

    @Override
    public void onStartListening() {
        tile = getQsTile();
        hider = new Hider(this);
        prefMgr = new PrefMgr(this);
        isProcessing = hider.getIsProcessingLiveData();

        try {
            isProcessing.observe(this, new TileUpdateObserver());
        } catch (IllegalStateException e) {
            Log.w(TAG, "UpdateObserver already exist: ", e);
        }

        updateTile();
        super.onStartListening();
    }

    @Override
    public void onClick() {
        unlockAndRun(() -> {
            Log.i(TAG, "Toggled tile.");
            if (prefMgr.getIsHidden()) {
                hider.unhide();
            } else {
                hider.hide();
            }
        });
    }
}
