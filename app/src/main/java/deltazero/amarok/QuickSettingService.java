package deltazero.amarok;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

public class QuickSettingService extends TileService {

    private static final String TAG = "TileService";

    Tile tile;
    Hider hider;
    PrefMgr prefMgr;
    boolean isProcessing = false;

    public class onHiderCallback implements Hider.HiderCallback {

        @Override
        public void onStart() {
            isProcessing = true;
            updateTile();
        }

        @Override
        public void onComplete() {
            isProcessing = false;
            updateTile();
        }
    }

    public void updateTile() {
        if (isProcessing) {
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

        updateTile();

        super.onStartListening();
    }

    @Override
    public void onClick() {
        Log.i(TAG, "Toggled tile.");
        if (prefMgr.getIsHidden()) {
            hider.unhide(new onHiderCallback());
        } else {
            hider.hide(new onHiderCallback());
        }
    }
}
