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

import deltazero.amarok.ui.SecurityAuthForQuickHideActivity;

public class QuickSettingService extends TileService {

    private static final String TAG = "TileService";

    private Tile tile;

    public void updateTile() {
    }

    @Override
    public void onStartListening() {
    }

    @Override
    public void onClick() {
    }
}
