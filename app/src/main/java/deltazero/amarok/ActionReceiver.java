package deltazero.amarok;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.util.Objects;

public class ActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("ActionReceiver", "New action received.");
        Hider hider = new Hider(context);

        if (Boolean.TRUE.equals(hider.getIsProcessingLiveData().getValue())) {
            Log.w("ActionReceiver", "Already processing. Ignore the new action.");
            return;
        }

        if (Objects.equals(intent.getAction(), "deltazero.amarok.HIDE")) {
            hider.hide();
        } else if (Objects.equals(intent.getAction(), "deltazero.amarok.UNHIDE")) {
            hider.unhide();
        } else {
            Log.w("ActionReceiver", "Invalid action: " + intent.getAction());
            Toast.makeText(context, context.getString(R.string.invalid_action, intent.getAction()),
                    Toast.LENGTH_LONG).show();
        }
    }
}
