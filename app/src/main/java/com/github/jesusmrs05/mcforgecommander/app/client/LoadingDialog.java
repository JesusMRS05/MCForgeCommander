package com.github.jesusmrs05.mcforgecommander.app.client;

import android.app.Dialog;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.github.jesusmrs05.mcforgecommander.R;

public class LoadingDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Translucent_NoTitleBar);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_loading, null);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(view);
        dialog.setCancelable(false);

        dialog.setOnKeyListener((d, keyCode, event) -> {
            return keyCode == KeyEvent.KEYCODE_BACK;
        });

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }

        return dialog;
    }
}
