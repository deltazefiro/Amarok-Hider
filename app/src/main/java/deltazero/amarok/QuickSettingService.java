package deltazero.amarok;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

public class QuickSettingService extends TileService {

    private static final String TAG = "TileService";

    private Tile tile;
    private Hider hider;
    private PrefMgr prefMgr;
    private MutableLiveData<Boolean> isProcessing;
    // private TileUpdateObserver updateObserver;

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
            isProcessing.observeForever(new TileUpdateObserver());
        } catch (IllegalStateException e) {
            Log.w(TAG, "UpdateObserver already exist: ", e);
        }

        updateTile();
        super.onStartListening();
    }

    // @Override
    // public void onStopListening() {
    //     isProcessing.removeObserver(updateObserver);
    //     super.onStopListening();
    // }

    @Override
    public void onClick() {
        Log.i(TAG, "Toggled tile.");
        if (prefMgr.getIsHidden()) {
            hider.unhide();
        } else {
            hider.hide();
        }
    }
}
