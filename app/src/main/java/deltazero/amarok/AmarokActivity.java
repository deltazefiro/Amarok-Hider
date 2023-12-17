package deltazero.amarok;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import deltazero.amarok.ui.SecurityAuthActivity;
import deltazero.amarok.utils.SecurityAuth;

public class AmarokActivity extends AppCompatActivity {
    @Override
    protected void onResume() {
        if (SecurityAuth.isUnlockRequired())
            startActivity(new Intent(this, SecurityAuthActivity.class));
        super.onResume();
    }
}
