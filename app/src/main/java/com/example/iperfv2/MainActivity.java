package com.example.iperfv2;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.system.ErrnoException;
import android.system.Os;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ToggleButton;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
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
    private Process process;
    private ToggleButton toggle;

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_about:

                AlertDialog alertDialog = new AlertDialog.Builder(this).create(); //Read Update
                alertDialog.setTitle("Information");
                alertDialog.setMessage("" +
                        "Application made by senior interns at Sony Mobile Communication for the Access Technology department during" +
                        "spring 2021." +
                        "");

                alertDialog.show();  //<-- See This!
                return true;
            case R.id.menu_clear:
                clearView();
                return true;
            case R.id.menu_presets:
                // bring up presets window
                return true;
            case R.id.menu_save:
                saveLog();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

        toggle = (ToggleButton) findViewById(R.id.toggleButton);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    clearView();
                    executeTest(inputText.getText().toString());
                    buttonView.setBackgroundColor(Color.GREEN);
                } else {
                    process.destroy();
                    buttonView.setBackgroundColor(Color.RED);
                }
            }
        });

        chart = findViewById(R.id.chart1);
        chart.setBackgroundColor(Color.LTGRAY);
        chart.getDescription().setEnabled(false);
    }


    // //   //  //  //  //  //  //
    //  Process related methods //
    //  //  //  //  //  //  //  //
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

                    formatStringToGraph(lineStr + "\r\n");
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

                process.waitFor();
                toggle.setChecked(false);
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

    // //   //  //  //
    //  Misc methods //
    //  //  //  //  //

    public void clearView() {
        chart.clear();
        LineData data = new LineData();
        chart.setData(data);

        testAdapter.clear();
        mRecycler.removeAllViewsInLayout();
        builder.setLength(0);
    }

    public void saveLog() {
        String filename = Calendar.getInstance().getTime().toString();
        String fileContents = builder.toString();

        FileOutputStream fos = null;

        try {
            fos = openFileOutput(filename, MODE_PRIVATE);
            fos.write(fileContents.getBytes());

            Message msg = testHandler.obtainMessage();
            msg.obj = "Log to " + getFilesDir() + "\r\n";
            msg.what = 10;
            msg.sendToTarget();

            //chart.saveToGallery(filename);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
        // Alternative to store to externaldrive NOT WORKING
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

    //Catches msg checks for graph input and formats properly
    public void formatStringToGraph(String msg) {
        Pattern p = Pattern.compile("(\\d*)\\d*\\s(\\w)bits\\/sec");   //"(\\d*)\\.\\d*\\s\\wbits\\/sec"
        Matcher m = p.matcher(msg);
        while (m.find()) {
            int value = Integer.parseInt(m.group(1));
            addEntry(value);
        }
    }

    private void addEntry(int value) {

        LineData data = chart.getData();
        chart.setData(data);

        if (data != null) {

            ILineDataSet set = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), (float) value), 0);
            data.notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.invalidate();

            // limit the number of visible entries
            //chart.setVisibleXRangeMaximum(0);
            // chart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            //chart.moveViewToX(data.getEntryCount());
        }
    }

    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "Download");
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(Color.WHITE);
        set.setLineWidth(2f);
        set.setFillAlpha(65);
        set.setDrawValues(false);


        return set;
    }


}

