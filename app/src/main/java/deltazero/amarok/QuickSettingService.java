package deltazero.amarok;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

public class QuickSettingService extends TileService {

    private static final String TAG = "MyTileService";
    Tile tile;
    Hider hider;
    PrefMgr prefMgr;

    @Override
    public void onStartListening() {
        tile = getQsTile();

        hider = new Hider(this);
        prefMgr = new PrefMgr(this);

        if (prefMgr.getIsHidden()) {
            tile.setState(Tile.STATE_INACTIVE);
        } else {
            tile.setState(Tile.STATE_ACTIVE);
        }
        super.onStartListening();
    }

    @Override
    public void onClick() {
        Log.d(TAG, "QS tile is clicked!");
        if (prefMgr.getIsHidden()) {
            hider.syncUnhide();
            tile.setState(Tile.STATE_ACTIVE);
        } else {
            hider.syncHide();
            tile.setState(Tile.STATE_INACTIVE);
        }
        tile.updateTile();
    }
}
