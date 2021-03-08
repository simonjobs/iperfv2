package com.example.iperfv2;

import android.os.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;

public class TestTask implements Runnable {

    private String cmd;
    private TestHandler testHandler;
    private long delay;
    private MainActivity activity;

    private Process process;

    public TestTask(String cmd, TestHandler testHandler, int delay, MainActivity activity) {
        this.cmd = cmd;
        this.testHandler = testHandler;
        this.delay = delay;
        this.activity = activity;
    }

    @Override
    public void run() {
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

                activity.formatGraph(lineStr + "\r\n");
                activity.getBuilder().append(lineStr + "\r\n");
            }
            while ((lineStr = errorReader.readLine()) != null) {

                // receive
                Message msg = testHandler.obtainMessage();
                msg.obj = lineStr + "\r\n";
                msg.what = 10;
                msg.sendToTarget();
            }
            Thread.sleep(delay * 1000);

            process.waitFor();
            activity.getToggle().setChecked(false);
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

    public Process getProcess() {
        return process;
    }
}