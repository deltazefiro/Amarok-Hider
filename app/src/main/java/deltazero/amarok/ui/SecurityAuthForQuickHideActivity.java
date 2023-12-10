package deltazero.amarok.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import deltazero.amarok.Hider;
import deltazero.amarok.R;
import deltazero.amarok.utils.SecurityAuth;

/**
 * An empty & transparent activity with a SecurityAuth dialog only.
 * If the SecurityAuth is passed, unhide files and apps. Otherwise, finish the activity.
 */
public class SecurityAuthForQuickHideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_empty);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("SecurityAuth", "Start SecurityAuthForQuickHideActivity");
        new SecurityAuth(this, succeed -> {
            if (succeed)
                Hider.unhide(this);
            finish();
        }).authenticate();
    }
}