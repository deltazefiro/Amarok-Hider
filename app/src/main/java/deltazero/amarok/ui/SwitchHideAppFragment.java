package deltazero.amarok.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import deltazero.amarok.AppHider.AppHiderBase;
import deltazero.amarok.AppHider.DsmAppHider;
import deltazero.amarok.AppHider.NoneAppHider;
import deltazero.amarok.AppHider.RootAppHider;
import deltazero.amarok.AppHider.ShizukuHider;
import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;

public class SwitchHideAppFragment extends DialogFragment {

    PrefMgr prefMgr;
    RadioButton rbDisabled, rbRoot, rbShizuku, rbDSM;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialog_apphider_modes, container, false);

        rbDisabled = view.findViewById(R.id.dialog_apphider_radio_disabled);
        rbRoot = view.findViewById(R.id.dialog_apphider_radio_root);
        rbShizuku = view.findViewById(R.id.dialog_apphider_radio_shizuku);
        rbDSM = view.findViewById(R.id.dialog_apphider_radio_dsm);

        prefMgr = new PrefMgr(view.getContext());
        setCheckedRadioButton(prefMgr.getAppHider().getClass());

        return view;
    }


    public void onCheckRadioButton(View view) {
        int buttonID = view.getId();
        if (((RadioButton) view).isChecked()) {
            if (buttonID == R.id.dialog_apphider_radio_disabled) {
                new NoneAppHider(getContext()).active(this::onActivationCallback);
            } else if (buttonID == R.id.dialog_apphider_radio_root) {
                new RootAppHider(getContext()).active(this::onActivationCallback);
            } else if (buttonID == R.id.dialog_apphider_radio_shizuku) {
                new ShizukuHider(getContext()).active(this::onActivationCallback);
            } else if (buttonID == R.id.dialog_apphider_radio_dsm) {
                new DsmAppHider(getContext()).active(this::onActivationCallback);
            }
        }
    }

    public void onActivationCallback(Class<? extends AppHiderBase> appHider, boolean success, int msgResID) {
        if (success) {
            prefMgr.setAppHiderMode(appHider);
            setCheckedRadioButton(appHider);
        } else {
            assert getContext() != null;
            prefMgr.setAppHiderMode(NoneAppHider.class);
            setCheckedRadioButton(NoneAppHider.class);
            new MaterialAlertDialogBuilder(getContext())
                    .setTitle(R.string.apphider_not_ava_title)
                    .setMessage(msgResID)
                    .setPositiveButton(getString(R.string.ok), null)
                    .setNeutralButton(R.string.help, (dialog, which) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://deltazefiro.github.io/Amarok-doc/hideapp.html"))))
                    .show();
        }
    }


    private void setCheckedRadioButton(Class<? extends AppHiderBase> appHider) {
        // Thank you, ChatGPT!
        if (appHider.isAssignableFrom(NoneAppHider.class)) {
            rbDisabled.setChecked(true);
            rbRoot.setChecked(false);
            rbShizuku.setChecked(false);
            rbDSM.setChecked(false);
        } else if (appHider.isAssignableFrom(RootAppHider.class)) {
            rbDisabled.setChecked(false);
            rbRoot.setChecked(true);
            rbShizuku.setChecked(false);
            rbDSM.setChecked(false);
        } else if (appHider.isAssignableFrom(ShizukuHider.class)) {
            rbDisabled.setChecked(false);
            rbRoot.setChecked(false);
            rbShizuku.setChecked(true);
            rbDSM.setChecked(false);
        } else if (appHider.isAssignableFrom(DsmAppHider.class)) {
            rbDisabled.setChecked(false);
            rbRoot.setChecked(false);
            rbShizuku.setChecked(false);
            rbDSM.setChecked(true);
        }
    }
}
