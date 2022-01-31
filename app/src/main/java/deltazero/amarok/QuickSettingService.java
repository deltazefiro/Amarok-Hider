package deltazero.amarok;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

public class QuickSettingService extends TileService {

    private static final String TAG = "MyTileService";
    Tile tile;
    Hider hider;

    @Override
    public void onStartListening() {
        tile = getQsTile();
        hider = new Hider(this);
        if (hider.getIsNight()) {
            tile.setState(Tile.STATE_ACTIVE);
        } else {
            tile.setState(Tile.STATE_INACTIVE);
        }
        super.onStartListening();
    }

    @Override
    public void onClick() {
        Log.d(TAG, "QS tile is clicked!");
        if (tile.getState() == Tile.STATE_ACTIVE) {
            hider.dawn();
            tile.setState(Tile.STATE_INACTIVE);
        } else {
            hider.dusk();
            tile.setState(Tile.STATE_ACTIVE);
        }
        tile.updateTile();
    }
}
