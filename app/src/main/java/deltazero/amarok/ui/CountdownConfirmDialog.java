package deltazero.amarok.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import deltazero.amarok.R;

public class CountdownConfirmDialog {

    private final Context context;
    private final String title;
    private final String message;
    private final int countdownSeconds;
    private final Runnable onConfirmAction;
    private final Runnable onCancelAction;

    private CountdownConfirmDialog(Builder builder) {
        this.context = builder.context;
        this.title = builder.title;
        this.message = builder.message;
        this.countdownSeconds = builder.countdownSeconds;
        this.onConfirmAction = builder.onConfirmAction;
        this.onCancelAction = builder.onCancelAction;
    }

    public void show() {
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(context);
        dialogBuilder.setTitle(title);
        dialogBuilder.setMessage(message);

        // Positive button (initially disabled)
        dialogBuilder.setPositiveButton(android.R.string.ok, (d, which) -> {
            if (onConfirmAction != null) {
                onConfirmAction.run();
            }
        });

        // Negative button
        dialogBuilder.setNegativeButton(android.R.string.cancel, (d, which) -> {
            if (onCancelAction != null) {
                onCancelAction.run();
            }
        });

        var dialog = dialogBuilder.create();
        dialog.show();

        // Disable the positive button initially and set countdown text
        var positiveButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setEnabled(false);
            positiveButton.setText(context.getString(R.string.confirm_with_countdown, countdownSeconds));
        }

        // Start countdown
        final var finalDialog = dialog;
        Handler handler = new Handler(Looper.getMainLooper());
        final int[] countdown = {countdownSeconds};

        Runnable countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (countdown[0] > 0 && finalDialog.isShowing()) {
                    countdown[0]--;
                    var button = finalDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
                    if (button != null) {
                        if (countdown[0] > 0) {
                            button.setText(context.getString(R.string.confirm_with_countdown, countdown[0]));
                        } else {
                            button.setText(context.getString(R.string.confirm));
                            button.setEnabled(true);
                        }
                    }
                    if (countdown[0] > 0) {
                        handler.postDelayed(this, 1000);
                    }
                }
            }
        };

        handler.postDelayed(countdownRunnable, 1000);
    }

    public static class Builder {
        private final Context context;
        private String title = "";
        private String message = "";
        private int countdownSeconds = 5;
        private Runnable onConfirmAction;
        private Runnable onCancelAction;

        public Builder(@NonNull Context context) {
            this.context = context;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setTitle(int titleResId) {
            this.title = context.getString(titleResId);
            return this;
        }

        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder setMessage(int messageResId) {
            this.message = context.getString(messageResId);
            return this;
        }

        public Builder setCountdownSeconds(int seconds) {
            this.countdownSeconds = seconds;
            return this;
        }

        public Builder setOnConfirmAction(Runnable action) {
            this.onConfirmAction = action;
            return this;
        }

        public Builder setOnCancelAction(Runnable action) {
            this.onCancelAction = action;
            return this;
        }

        public CountdownConfirmDialog build() {
            return new CountdownConfirmDialog(this);
        }

        public void show() {
            build().show();
        }
    }
}

