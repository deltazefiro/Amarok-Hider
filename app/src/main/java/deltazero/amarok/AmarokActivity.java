package deltazero.amarok;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import java.util.List;

import deltazero.amarok.ui.CalendarActivity;
import deltazero.amarok.ui.SecurityAuthActivity;
import deltazero.amarok.utils.SecurityUtil;

public class AmarokActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Enable edge-to-edge display
        WindowCompat.enableEdgeToEdge(getWindow());
    }
    
    @Override
    protected void onStart() {
        if (PrefMgr.getBlockScreenshots())
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);

        if (PrefMgr.getHideFromRecents()) {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                List<ActivityManager.AppTask> tasks = am.getAppTasks();
                if (!tasks.isEmpty()) {
                    tasks.get(0).setExcludeFromRecents(true);
                }
            }
        }

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
