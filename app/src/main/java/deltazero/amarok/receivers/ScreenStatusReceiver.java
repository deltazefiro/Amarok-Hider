package deltazero.amarok.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import deltazero.amarok.utils.AutoHideUtil;
import deltazero.amarok.utils.SecurityUtil;

public class ScreenStatusReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        assert intent.getAction() != null;
        switch (intent.getAction()) {
            case Intent.ACTION_SCREEN_ON -> {
                Log.i("ScreenStatusReceiver", "Screen unlocked.");
                AutoHideUtil.cancelAutoHide(context);
            }
            case Intent.ACTION_SCREEN_OFF -> {
                Log.i("ScreenStatusReceiver", "Screen locked.");
                SecurityUtil.lockAndDisguise();
                AutoHideUtil.setAutoHide(context);
            }
        }
    }
}