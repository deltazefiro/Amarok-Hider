package deltazero.amarok.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import deltazero.amarok.Hider;
import deltazero.amarok.R;
import deltazero.amarok.ui.SecurityAuthForQSActivity;
import deltazero.amarok.utils.SecurityUtil;

public class ActionReceiver extends BroadcastReceiver {

    public static final String ACTION_HIDE = "deltazero.amarok.HIDE";
    public static final String ACTION_UNHIDE = "deltazero.amarok.UNHIDE";
    public static final String ACTION_TOGGLE = "deltazero.amarok.TOGGLE";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("ActionReceiver", "New action received.");

        if (Hider.state.getValue() == Hider.State.PROCESSING) {
            Log.w("ActionReceiver", "Already processing. Ignore the new action.");
            return;
        }

        String action = intent.getAction();
        if (action != null)
            switch (action) {
                case ACTION_HIDE:
                    Hider.hide(context);
                    return;
                case ACTION_UNHIDE:
                    if (SecurityUtil.isUnlockRequired())
                        context.startActivity(new Intent(context, SecurityAuthForQSActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    else
                        Hider.unhide(context);
                    return;
                case ACTION_TOGGLE:
                    if (Hider.getState() == Hider.State.VISIBLE)
                        Hider.hide(context);
                    else {
                        if (SecurityUtil.isUnlockRequired())
                            context.startActivity(new Intent(context, SecurityAuthForQSActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        else
                            Hider.unhide(context);
                    }
                    return;
            }

        Log.w("ActionReceiver", "Invalid action: " + intent.getAction());
        Toast.makeText(context, context.getString(R.string.invalid_action, intent.getAction()),
                Toast.LENGTH_LONG).show();

    }
}
