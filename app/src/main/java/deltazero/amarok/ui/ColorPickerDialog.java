package deltazero.amarok.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import deltazero.amarok.R;

public class ColorPickerDialog {

    private final Context context;
    private final String title;
    private final int currentColor;
    private final OnColorSelectedListener listener;

    // Predefined color palette
    private static final int[] PRESET_COLORS = {
            0xFFD1D1D1, // Light Grey (default)
            0xFFFFFFFF, // White
            0xFF000000, // Black
            0xFFFF5252, // Red
            0xFFFF4081, // Pink
            0xFFE040FB, // Purple
            0xFF7C4DFF, // Deep Purple
            0xFF536DFE, // Indigo
            0xFF448AFF, // Blue
            0xFF40C4FF, // Light Blue
            0xFF18FFFF, // Cyan
            0xFF64FFDA, // Teal
            0xFF69F0AE, // Green
            0xFFB2FF59, // Light Green
            0xFFEEFF41, // Lime
            0xFFFFFF00, // Yellow
            0xFFFFD740, // Amber
            0xFFFFAB40, // Orange
            0xFFFF6E40, // Deep Orange
    };

    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    private ColorPickerDialog(Builder builder) {
        this.context = builder.context;
        this.title = builder.title;
        this.currentColor = builder.currentColor;
        this.listener = builder.listener;
    }

    public void show() {
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(context);
        dialogBuilder.setTitle(title);

        // Create the color picker layout
        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);

        // Use responsive padding
        int horizontalPadding = (int) (24 * context.getResources().getDisplayMetrics().density);
        int verticalPadding = (int) (16 * context.getResources().getDisplayMetrics().density);
        mainLayout.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);

        // Calculate responsive grid parameters
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int availableWidth = screenWidth - (2 * horizontalPadding);

        // Calculate optimal swatch size and columns
        int minSwatchSize = (int) (40 * context.getResources().getDisplayMetrics().density);
        int maxSwatchSize = (int) (56 * context.getResources().getDisplayMetrics().density);
        int margin = (int) (8 * context.getResources().getDisplayMetrics().density);

        // Determine number of columns (try 5, then 4, then 3 if needed)
        int columns = 5;
        int size = (availableWidth - (columns + 1) * margin) / columns;

        if (size < minSwatchSize) {
            columns = 4;
            size = (availableWidth - (columns + 1) * margin) / columns;
        }
        if (size < minSwatchSize) {
            columns = 3;
            size = (availableWidth - (columns + 1) * margin) / columns;
        }

        // Cap at maximum size for very large screens
        if (size > maxSwatchSize) {
            size = maxSwatchSize;
        }

        // Create grid for color swatches
        GridLayout colorGrid = new GridLayout(context);
        colorGrid.setColumnCount(columns);
        colorGrid.setRowCount((int) Math.ceil(PRESET_COLORS.length / (double) columns));

        final int[] selectedColor = {currentColor};

        // Declare hex input early so it can be accessed in onClick listeners
        EditText hexInput = new EditText(context);
        hexInput.setHint(R.string.panic_button_color_hex_hint);
        hexInput.setText(String.format("%06X", (currentColor & 0xFFFFFF)));

        for (int color : PRESET_COLORS) {
            ImageView colorSwatch = new ImageView(context);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = size;
            params.height = size;
            params.setMargins(margin, margin, margin, margin);
            colorSwatch.setLayoutParams(params);

            // Create circular drawable
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(color);

            // Add border if this is the current color
            if (color == currentColor) {
                drawable.setStroke((int) (4 * context.getResources().getDisplayMetrics().density),
                        Color.BLACK);
            }

            colorSwatch.setImageDrawable(drawable);
            colorSwatch.setTag(color);

            colorSwatch.setOnClickListener(v -> {
                selectedColor[0] = (int) v.getTag();
                // Update hex input field to match selected color
                String hexColor = String.format("%06X", (selectedColor[0] & 0xFFFFFF));
                hexInput.setText(hexColor);
                hexInput.setSelection(hexColor.length()); // Move cursor to end to ensure update
                // Update all swatches to show selection
                for (int i = 0; i < colorGrid.getChildCount(); i++) {
                    ImageView swatch = (ImageView) colorGrid.getChildAt(i);
                    int swatchColor = (int) swatch.getTag();
                    GradientDrawable swatchDrawable = new GradientDrawable();
                    swatchDrawable.setShape(GradientDrawable.OVAL);
                    swatchDrawable.setColor(swatchColor);
                    if (swatchColor == selectedColor[0]) {
                        swatchDrawable.setStroke((int) (4 * context.getResources().getDisplayMetrics().density),
                                Color.BLACK);
                    }
                    swatch.setImageDrawable(swatchDrawable);
                }
            });

            colorGrid.addView(colorSwatch);
        }

        mainLayout.addView(colorGrid);

        // Add custom hex input with layout params
        LinearLayout.LayoutParams hexParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        hexParams.setMargins(0, margin * 2, 0, 0);
        hexInput.setLayoutParams(hexParams);
        mainLayout.addView(hexInput);

        dialogBuilder.setView(mainLayout);

        dialogBuilder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            // Try to parse hex input first
            String hexText = hexInput.getText().toString().trim();
            int finalColor = selectedColor[0];

            if (!hexText.isEmpty()) {
                try {
                    // Remove '#' if present
                    if (hexText.startsWith("#")) {
                        hexText = hexText.substring(1);
                    }
                    // Parse hex color
                    finalColor = 0xFF000000 | Integer.parseInt(hexText, 16);
                } catch (NumberFormatException e) {
                    Toast.makeText(context, R.string.panic_button_color_invalid_hex,
                            Toast.LENGTH_SHORT).show();
                    // Fall back to selected color
                }
            }

            if (listener != null) {
                listener.onColorSelected(finalColor);
            }
        });

        dialogBuilder.setNegativeButton(android.R.string.cancel, null);

        dialogBuilder.create().show();
    }

    public static class Builder {
        private final Context context;
        private String title = "";
        private int currentColor = 0xFFD1D1D1;
        private OnColorSelectedListener listener;

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

        public Builder setCurrentColor(int color) {
            this.currentColor = color;
            return this;
        }

        public Builder setOnColorSelectedListener(OnColorSelectedListener listener) {
            this.listener = listener;
            return this;
        }

        public ColorPickerDialog build() {
            return new ColorPickerDialog(this);
        }

        public void show() {
            build().show();
        }
    }
}
