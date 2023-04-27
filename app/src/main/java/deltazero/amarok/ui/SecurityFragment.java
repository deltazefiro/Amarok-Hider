package deltazero.amarok.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;

public class SecurityFragment extends BottomSheetDialogFragment {

    private final OnVerifiedCallback onVerifiedCallback;
    private TextInputEditText etPassword;
    private TextInputLayout tilPassword;
    private PrefMgr prefMgr;

    interface OnVerifiedCallback {
        public void onVerified(boolean succeed);
    }

    public SecurityFragment(PrefMgr prefMgr, OnVerifiedCallback onVerifiedCallback) {
        this.prefMgr = prefMgr;
        this.onVerifiedCallback = onVerifiedCallback;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        setCancelable(false);
        View fragmentView = inflater.inflate(R.layout.dialog_security, container, false);

        fragmentView.findViewById(R.id.security_dialog_bt_unlock).setOnClickListener(v -> verify());
        fragmentView.findViewById(R.id.security_dialog_bt_cancel).setOnClickListener(v -> onVerifiedCallback.onVerified(false));
        etPassword = fragmentView.findViewById(R.id.security_dialog_et_password_input);
        tilPassword = fragmentView.findViewById(R.id.security_dialog_til_password_input);

        // Clear error on text changed
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                tilPassword.setError(null);
                tilPassword.setErrorEnabled(false);
            }
        });

        return fragmentView;
    }

    private void verify() {
        if ("123".equals(etPassword.getText().toString())) {
            dismiss();
            onVerifiedCallback.onVerified(true);
        } else {
            tilPassword.setError(getText(R.string.password_error));
        }
    }

}
