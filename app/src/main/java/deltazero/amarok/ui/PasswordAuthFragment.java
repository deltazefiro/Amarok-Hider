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
import deltazero.amarok.utils.HashUtil;

public class PasswordAuthFragment extends BottomSheetDialogFragment {

    private OnVerifiedCallback onVerifiedCallback;
    private TextInputEditText etPassword;
    private TextInputLayout tilPassword;

    public interface OnVerifiedCallback {
        void onVerified(boolean succeed);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        setCancelable(false);
        View fragmentView = inflater.inflate(R.layout.dialog_security, container, false);

        fragmentView.findViewById(R.id.security_dialog_bt_unlock).setOnClickListener(v -> verify());
        fragmentView.findViewById(R.id.security_dialog_bt_cancel).setOnClickListener(v -> {
            if (onVerifiedCallback != null) onVerifiedCallback.onVerified(false);
        });
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

    public PasswordAuthFragment setOnVerifiedCallback(OnVerifiedCallback onVerifiedCallback) {
        this.onVerifiedCallback = onVerifiedCallback;
        return this;
    }

    private void verify() {

        String password = PrefMgr.getAmarokPassword();
        assert etPassword.getText() != null;

        if (password == null || HashUtil.calculateHash(etPassword.getText().toString()).equals(password)) {
            if (onVerifiedCallback != null) onVerifiedCallback.onVerified(true);
            dismiss();
        } else {
            tilPassword.setError(getText(R.string.password_incorrect));
        }
    }

}
