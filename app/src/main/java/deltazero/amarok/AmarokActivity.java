package deltazero.amarok;

import android.content.Intent;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import deltazero.amarok.ui.CalendarActivity;
import deltazero.amarok.ui.SecurityAuthActivity;
import deltazero.amarok.utils.SecurityUtil;

public class AmarokActivity extends AppCompatActivity {
    @Override
    protected void onStart() {
        if (PrefMgr.getBlockScreenshots())
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
        super.onStart();
    }

    @Override
    protected void onResume() {
        if (SecurityUtil.isDisguiseNeeded())
            startActivity(new Intent(this, CalendarActivity.class));
        else if (SecurityUtil.isUnlockRequired())
            startActivity(new Intent(this, SecurityAuthActivity.class));
        super.onResume();
    }
}
