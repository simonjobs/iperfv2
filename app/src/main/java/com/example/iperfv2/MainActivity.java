package com.example.iperfv2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.system.ErrnoException;
import android.system.Os;
import android.view.View;
import android.widget.EditText;

import com.example.iperfv2.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mRecycler;
    private EditText inputText;
    private ExecutorService executorService;

    private TestAdapter testAdapter;
    private TestHandler testHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Os.setenv("TMPDIR", "/data/data/com.example.iperfv2/cache/", true);
        } catch (ErrnoException e) {
            e.printStackTrace();
        }
        setContentView(R.layout.activity_main);
        initViews();
    }

    public void initViews() {
        mRecycler = (RecyclerView) findViewById(R.id.mRecycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecycler.setLayoutManager(layoutManager);
        testAdapter = new TestAdapter();
        mRecycler.setAdapter(testAdapter);
        testHandler = new TestHandler(this);
        inputText = findViewById(R.id.inputText);
        findViewById(R.id.btnEnter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println(getCommand());
                executeTest(getCommand());
            }
        });

    }

    /*
    public void buttonPress(View view) {
        executeTest(getCommand());
    }
     */

    public void executeTest(String cmd) {
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Thread(new testTask(cmd, testHandler, 1)));
    }

    @Override
    public void onDestroy() {
        if (testHandler != null) {
            testHandler.removeCallbacksAndMessages(null);
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
        super.onDestroy();
    }

    private static class TestHandler extends Handler {
        private WeakReference<MainActivity> weakReference;

        public TestHandler(MainActivity activity) {
            this.weakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch(msg.what) {
                case 10:
                    String resultMsg = (String) msg.obj;
                    weakReference.get().testAdapter.addString(resultMsg);
                    weakReference.get().mRecycler.scrollToPosition(weakReference.get().testAdapter.getItemCount() - 1);
                    break;
                default:
                    break;
            }
        }
    }

    private class testTask implements Runnable {
        private String cmd;
        private TestHandler testHandler;
        private long delay;

        public testTask(String cmd, TestHandler testHandler, int delay) {
            this.cmd = cmd;
            this.testHandler = testHandler;
            this.delay = delay;
        }

        @Override
        public void run() {
            Process process = null;
            BufferedReader successReader = null;
            BufferedReader errorReader = null;

            try {
            // test
            process = Runtime.getRuntime().exec(cmd);
            // success
            successReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            // error
            errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String lineStr;

            while ((lineStr = successReader.readLine()) != null) {

                // receive
                Message msg = testHandler.obtainMessage();
                msg.obj = lineStr + "\r\n";
                msg.what = 10;
                msg.sendToTarget();
            }
            while ((lineStr = errorReader.readLine()) != null) {

                // receive
                Message msg = testHandler.obtainMessage();
                msg.obj = lineStr + "\r\n";
                msg.what = 10;
                msg.sendToTarget();
            }
            Thread.sleep(delay * 1000);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {

                if (successReader != null) {
                    successReader.close();
                }
                if (errorReader != null) {
                    errorReader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (process != null) {
                process.destroy();
            }
        }
        }
    }



    public String getCommand() {
        return inputText.getText().toString();
    }
}