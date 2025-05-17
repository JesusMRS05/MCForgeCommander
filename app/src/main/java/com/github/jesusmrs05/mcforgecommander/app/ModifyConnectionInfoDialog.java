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

import androidx.annotation.NonNull;

import com.github.jesusmrs05.mcforgecommander.R;
import com.google.android.material.textfield.TextInputEditText;

public class ModifyConnectionInfoDialog extends Dialog {

    private TextInputEditText tietHost, tietPort, tietPass;
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
            params.width = (metrics.widthPixels < 1100) ?
                    ViewGroup.LayoutParams.MATCH_PARENT :
                    1150;

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

        btnCancel.setOnClickListener(v -> {
            dismiss();
        });

        btnAccept.setOnClickListener(v -> {
            connectionInfo.setHost(tietHost.getText().toString());
            connectionInfo.setPort(Integer.parseInt(tietPort.getText().toString()));
            connectionInfo.setPassword(tietPass.getText().toString());
            dismiss();
            callback.run();
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
