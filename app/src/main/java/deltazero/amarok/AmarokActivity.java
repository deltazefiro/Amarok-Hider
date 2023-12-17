package deltazero.amarok;

import android.content.Intent;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;

import deltazero.amarok.ui.CalendarActivity;
import deltazero.amarok.ui.SecurityAuthActivity;
import deltazero.amarok.utils.SecurityUtil;

public class AmarokActivity extends AppCompatActivity {
    @Override
    protected void onResume() {
        if (SecurityUtil.isDisguiseNeeded())
            startActivity(new Intent(this, CalendarActivity.class));
        else if (SecurityUtil.isUnlockRequired())
            startActivity(new Intent(this, SecurityAuthActivity.class));
        super.onResume();
    }
}
