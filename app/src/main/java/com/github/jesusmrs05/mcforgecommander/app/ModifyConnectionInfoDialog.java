package com.github.jesusmrs05.mcforgecommander.app;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.jesusmrs05.mcforgecommander.R;
import com.google.android.material.textfield.TextInputEditText;

public class ModifyConnectionInfoDialog extends Dialog {

    private TextInputEditText tietHost, tietPort, tietPass, tietFPS, tietImageQuality;
    private TextView btnCancel, btnAccept;
    private ScrollView scrollView;
    private ConnectionInfo connectionInfo;
    private Runnable callback;

    public ModifyConnectionInfoDialog(@NonNull Context context, ConnectionInfo connectionInfo, Runnable callback) {
        super(context);
        this.connectionInfo = connectionInfo;
        this.callback = callback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_modify_connection_info);
        Window window = getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            DisplayMetrics metrics = new DisplayMetrics();
            window.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            /*params.width = (metrics.widthPixels < 1100) ?
                    ViewGroup.LayoutParams.MATCH_PARENT :
                    1150;*/

            params.width = ViewGroup.LayoutParams.MATCH_PARENT;

            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        scrollView = findViewById(R.id.scrollView);
        btnCancel = findViewById(R.id.btnCancel);
        btnAccept = findViewById(R.id.btnAccept);
        tietHost = findViewById(R.id.tietHost);
        tietPort = findViewById(R.id.tietPort);
        tietPass = findViewById(R.id.tietPass);
        tietFPS = findViewById(R.id.tietFPS);
        tietImageQuality = findViewById(R.id.tietImageQuality);

        tietHost.setText(connectionInfo.getHost());
        tietPort.setText(String.valueOf(connectionInfo.getPort()));
        tietPass.setText(connectionInfo.getPassword());
        tietFPS.setText(String.valueOf(connectionInfo.getFps()));
        tietImageQuality.setText(String.valueOf(connectionInfo.getImageQuality()));

        btnCancel.setOnClickListener(v -> {
            dismiss();
        });

        btnAccept.setOnClickListener(v -> {
            if (tietHost.getText().toString().isEmpty() || tietPort.getText().toString().isEmpty() || tietPass.getText().toString().isEmpty() || tietFPS.getText().toString().isEmpty() || tietImageQuality.getText().toString().isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else if (Integer.parseInt(tietPort.getText().toString()) < 1 || Integer.parseInt(tietPort.getText().toString()) > 65535) {
                Toast.makeText(getContext(), "Port must be between 1 and 65535", Toast.LENGTH_SHORT).show();
            } else if (Integer.parseInt(tietFPS.getText().toString()) < 1 || Integer.parseInt(tietFPS.getText().toString()) > 30) {
                Toast.makeText(getContext(), "FPS must be between 1 and 30", Toast.LENGTH_SHORT).show();
            } else if (Float.parseFloat(tietImageQuality.getText().toString()) < 0.1 || Float.parseFloat(tietImageQuality.getText().toString()) > 1) {
                Toast.makeText(getContext(), "Image quality must be between 0.1 and 1", Toast.LENGTH_SHORT).show();
            }else {
                connectionInfo.setHost(tietHost.getText().toString());
                connectionInfo.setPort(Integer.parseInt(tietPort.getText().toString()));
                connectionInfo.setPassword(tietPass.getText().toString());
                connectionInfo.setFps(Integer.parseInt(tietFPS.getText().toString()));
                connectionInfo.setImageQuality(Float.parseFloat(tietImageQuality.getText().toString()));
                dismiss();
                callback.run();
            }
        });

        setupAutoScrollOnFocus(tietHost);
        setupAutoScrollOnFocus(tietPort);
        setupAutoScrollOnFocus(tietPass);

        tietHost.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                tietPort.requestFocus();
                return true;
            }
            return false;
        });

        tietPort.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                tietPass.requestFocus();
                return true;
            }
            return false;
        });

        tietPass.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btnAccept.performClick();
                return true;
            }
            return false;
        });
    }

    private void setupAutoScrollOnFocus(TextInputEditText editText) {
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollView.post(() -> {
                    Rect scrollBounds = new Rect();
                    scrollView.getHitRect(scrollBounds);

                    if (!v.getLocalVisibleRect(scrollBounds)) {
                        int[] location = new int[2];
                        v.getLocationInWindow(location);
                        int fieldBottom = location[1] + v.getHeight();

                        DisplayMetrics metrics = new DisplayMetrics();
                        getWindow().getWindowManager().getDefaultDisplay().getMetrics(metrics);
                        int keyboardHeightEstimate = metrics.heightPixels / 3;

                        int targetY = fieldBottom - keyboardHeightEstimate;

                        scrollView.smoothScrollTo(0, Math.max(targetY, 0));
                    }
                });
            }
        });
    }
}
