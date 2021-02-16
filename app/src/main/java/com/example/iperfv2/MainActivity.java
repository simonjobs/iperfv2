package com.example.iperfv2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.system.ErrnoException;
import android.system.Os;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.example.iperfv2.*;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.*;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mRecycler;
    private EditText inputText;
    private ExecutorService executorService;

    private TestAdapter testAdapter;
    private TestHandler testHandler;

    private StringBuilder builder;
    private LineChart chart;

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
        builder = new StringBuilder();
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
                executeTest(getCommand());
            }
        });
        chart = findViewById(R.id.chart1);
        chart.setBackgroundColor(Color.LTGRAY);
        chart.getDescription().setEnabled(false);
        LineData data = new LineData();
        chart.setData(data);
    }

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
            switch (msg.what) {
                case 10:
                    String resultMsg = (String) msg.obj;
                    formatStringToGraph(resultMsg);
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
            builder.setLength(0);

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

                    builder.append(lineStr + "\r\n");
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


    //Catches msg checks for graph input and formats properly
    //VERY NOT DONE
    public static void formatStringToGraph(String msg) {
        ArrayList<Integer> list = new ArrayList<>();
        msg = msg.trim();
        String word = msg.substring(msg.lastIndexOf(" ") + 1);
        word = word.trim();
        System.out.println(word);
        if (word.equals("Kbytes")) {
            System.out.println(word);
            Pattern p = Pattern.compile("(\\d*)\\.*\\d*");
            Matcher m = p.matcher(msg);
            while (m.find()) {
                list.add(Integer.parseInt(m.group()));
                System.out.println(m.group());
            }
            System.out.println(list);
        }
    }

    public void saveLog(View view) {
        String filename = Calendar.getInstance().getTime().toString();
        String fileContents = "Hello world"; //builder.toString();

        FileOutputStream fos = null;

        try {
            fos = openFileOutput(filename, MODE_PRIVATE);
            fos.write(fileContents.getBytes());

            Message msg = testHandler.obtainMessage();
            msg.obj = "Saved to " + getFilesDir() + "\r\n";
            msg.what = 10;
            msg.sendToTarget();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*

        try {
            File extBaseDir = Environment.getExternalStorageDirectory();
            File file = new File(extBaseDir.getAbsolutePath() + "/iPerf/filename.txt");
            file.mkdirs();

            String filePath = file.getAbsolutePath();
            FileOutputStream out = null;

            out = new FileOutputStream(filePath);
            out.write(fileContents.getBytes());
            out.flush();
            out.close();

            Message msg = testHandler.obtainMessage();
            msg.obj = "Saved to " + filePath + " as: " + filename + "\r\n";
            msg.what = 10;
            msg.sendToTarget();

            }catch (Exception e) {
            e.printStackTrace();
        }

         */


    }

    public void entryButton(View view) {
        int i = Integer.parseInt(inputText.getText().toString());
        addEntry(i);
    }

    private void addEntry(int value) {

        LineData data = chart.getData();

        if (data != null) {

            ILineDataSet set = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), (float) value), 0);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            chart.notifyDataSetChanged();

            // limit the number of visible entries
            chart.setVisibleXRangeMaximum(0);
            // chart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            chart.moveViewToX(data.getEntryCount());
        }
    }

    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "Download");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(Color.WHITE);
        set.setLineWidth(2f);
        set.setCircleRadius(4f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
    }
}

