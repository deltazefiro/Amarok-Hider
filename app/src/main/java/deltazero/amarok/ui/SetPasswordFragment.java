package deltazero.amarok.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

import deltazero.amarok.R;

public class SetPasswordFragment extends BottomSheetDialogFragment {

    public interface OnSetPasswordCallback {
        public void onSetPassword(@Nullable String password);
    }

    private TextInputLayout etlPassword;
    private TextInputLayout etlConfirmPassword;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    private MaterialButton btOk, btCancel;
    private OnSetPasswordCallback callback;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_set_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etlPassword = view.findViewById(R.id.set_password_dialog_til_password);
        etlConfirmPassword = view.findViewById(R.id.set_password_dialog_til_confirm_password);
        etPassword = view.findViewById(R.id.set_password_dialog_et_password);
        etConfirmPassword = view.findViewById(R.id.set_password_dialog_et_confirm_password);
        btCancel = view.findViewById(R.id.set_password_dialog_bt_cancel);
        btOk = view.findViewById(R.id.set_password_dialog_bt_ok);

        btOk.setOnClickListener(v -> {
            String password = Objects.requireNonNull(etPassword.getText()).toString();
            String confirmPassword = Objects.requireNonNull(etConfirmPassword.getText()).toString();

            if (password.length() < 3 || password.length() > 15) {
                etlPassword.setError(getString(R.string.password_length_error));
                return;
            }

            if (!password.equals(confirmPassword)) {
                etlConfirmPassword.setError(getString(R.string.password_mismatch_error));
                return;
            }

            callback.onSetPassword(password);
            dismiss();
        });

        btCancel.setOnClickListener(v -> {
            if (callback != null) callback.onSetPassword(null);
            dismiss();
        });

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                etlPassword.setError(null);
                etlConfirmPassword.setError(null);
                etlPassword.setErrorEnabled(false);
                etlConfirmPassword.setErrorEnabled(false);
            }
        });
    }

    public SetPasswordFragment setCallback(OnSetPasswordCallback callback) {
        this.callback = callback;
        return this;
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        if (callback != null) callback.onSetPassword(null);
        super.onCancel(dialog);
    }
}
