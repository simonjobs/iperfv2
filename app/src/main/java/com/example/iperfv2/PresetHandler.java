package com.example.iperfv2;

import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

// Handles presetlist messages used to create list
public class PresetHandler extends Handler {
    private MainActivity activity;

    public PresetHandler(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case 10:
                String resultMsg = (String) msg.obj;
                activity.getPresetAdapter().addString(resultMsg);
                activity.getpRecycler().scrollToPosition(activity.getPresetAdapter().getItemCount() - 1);
                break;
            default:
                break;
        }
    }
}