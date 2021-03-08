package com.example.iperfv2;

import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

// Handles output messages from console
public class TestHandler extends Handler {
    private WeakReference<MainActivity> weakReference;
    private TestAdapter testAdapter = new TestAdapter();

    public TestHandler(MainActivity activity) {
        this.weakReference = new WeakReference<>(activity);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case 10:
                String resultMsg = (String) msg.obj;
                weakReference.get().getTestAdapter().addString(resultMsg);
                weakReference.get().getmRecycler().scrollToPosition(weakReference.get().getTestAdapter().getItemCount() - 1);
                break;
            default:
                break;
        }
    }
}
