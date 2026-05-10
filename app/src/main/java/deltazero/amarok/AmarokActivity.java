package deltazero.amarok;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import deltazero.amarok.core.SettingsSnapshot;
import deltazero.amarok.ui.CalendarActivity;
import deltazero.amarok.ui.SecurityAuthActivity;
import deltazero.amarok.utils.SecurityUtil;
import java.util.List;

public class AmarokActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Enable edge-to-edge display
    EdgeToEdge.enable(this);
  }

  @Override
  protected void onStart() {
    SettingsSnapshot settings =
        ((AmarokApplication) getApplication()).getSettingsRepo().getSettings().getValue();
    if (settings.getBlockScreenshots())
      getWindow()
          .setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

    if (settings.getHideFromRecents()) {
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
    AmarokApplication app = (AmarokApplication) getApplication();
    if (SecurityUtil.isDisguiseNeeded(app)) startActivity(new Intent(this, CalendarActivity.class));
    else if (SecurityUtil.isUnlockRequired(app))
      startActivity(new Intent(this, SecurityAuthActivity.class));
    super.onResume();
  }
}
