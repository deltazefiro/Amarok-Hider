package deltazero.amarok.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import deltazero.amarok.utils.SecurityUtil;

public class ScreenLockReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("ScreenLockReceiver", "Screen locked");
        SecurityUtil.lockAndDisguise();
    }
}