package com.example.iperfv2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.system.ErrnoException;
import android.system.Os;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.*;

public class MainActivity extends AppCompatActivity implements PresetAdapter.ListItemClickListener, ActivityCompat.OnRequestPermissionsResultCallback{

    private ViewFlipper flipper;

    private RecyclerView mRecycler;
    private EditText inputText;
    private ToggleButton toggle;
    private LineChart chart;
    private TestAdapter testAdapter;
    private TestHandler testHandler;

    public RecyclerView pRecycler;
    private EditText loadText;
    private PresetHandler presetHandler;
    private PresetAdapter presetAdapter;

    private ExecutorService executorService;
    private StringBuilder builder;
    private Process process;
    private ArrayList<String> presets;


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
                flipper.showNext();
                return true;
            case R.id.menu_save:
                try {
                    saveTest();
                    Toast.makeText(this, "Log saved successfully!", Toast.LENGTH_SHORT);
                } catch (IllegalAccessError e) {
                    throw e;
                }
                return true;
            case R.id.menu_access:
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void initViews() {
        flipper = findViewById(R.id.myViewFlipper);
        builder = new StringBuilder();
        presets = new ArrayList<>();

        mRecycler = (RecyclerView) findViewById(R.id.mRecycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecycler.setLayoutManager(layoutManager);
        testAdapter = new TestAdapter();
        mRecycler.setAdapter(testAdapter);
        testHandler = new TestHandler(this);
        inputText = findViewById(R.id.inputText);

        pRecycler = (RecyclerView) findViewById(R.id.pRecycler);
        LinearLayoutManager layoutManager1 = new LinearLayoutManager(this);
        pRecycler.setLayoutManager(layoutManager1);
        presetAdapter = new PresetAdapter(this);
        pRecycler.setAdapter(presetAdapter);
        presetHandler = new PresetHandler(this);
        loadText = findViewById(R.id.filePath);

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

        Button button = (Button) findViewById(R.id.loadButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                loadPresets();
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

    @Override
    public void onListItemClick(int position) {

        inputText.setText(presets.get(position));
        flipper.showNext();


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

    private static class PresetHandler extends Handler {
        private WeakReference<MainActivity> weakReference;

        public PresetHandler(MainActivity activity) {
            this.weakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case 10:
                    String resultMsg = (String) msg.obj;
                    weakReference.get().presetAdapter.addString(resultMsg);
                    weakReference.get().pRecycler.scrollToPosition(weakReference.get().presetAdapter.getItemCount() - 1);
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


    public void loadPresets() {
        String preset;
        try {
            InputStream in = getResources().openRawResource(R.raw.presets);
            InputStreamReader inputStreamReader = new InputStreamReader(in);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            while ((preset = bufferedReader.readLine()) != null) {

                presets.add(preset);
                Message msg = presetHandler.obtainMessage();
                msg.obj = preset + "\r\n";
                msg.what = 10;
                msg.sendToTarget();
            }
            inputStreamReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }



    }

    public void clearView() {
        chart.clear();
        LineData data = new LineData();
        chart.setData(data);

        testAdapter.clear();
        mRecycler.removeAllViewsInLayout();
        builder.setLength(0);
    }

    public void saveTest() {
        String filename = Calendar.getInstance().getTime().toString();
        String fileContents = builder.toString();
        chart.saveToGallery(filename.substring(17,19));

        /*
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

        */
        // Alternative to store to externaldrive NOT WORKING
        try {
            File extBaseDir = Environment.getExternalStorageDirectory();
            File file = new File(extBaseDir.getAbsolutePath() + "/iPerf/");
            if (!file.exists()) {
                file.mkdirs();
            } else {
                file.delete();
                file.mkdirs();
            }

            String filePath = file.getAbsolutePath();
            FileOutputStream out = null;

            out = new FileOutputStream(filePath + "/" + filename.substring(17,19) + ".txt");
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
    }

    //Catches msg checks for graph input and formats properly
    public void formatStringToGraph(String msg) {
        Pattern p = Pattern.compile("(\\d*\\.*\\d*)\\d*\\s(\\w*)bits\\/sec");    //(\\d*)\\d*\\s(\\w)bits\\/sec"), "(\\d*)\\.\\d*\\s\\wbits\\/sec"
        Matcher m = p.matcher(msg);
        while (m.find()) {
            float value = Float.valueOf(m.group(1));
            addEntry(value);
        }
    }

    private void addEntry(float value) {

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
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(Color.WHITE);
        set.setLineWidth(2f);
        set.setFillAlpha(65);
        set.setDrawValues(false);


        return set;
    }

    //REQUEST PERMISSION TO SAVE CHART IMAGE CODE
    private static final int PERMISSION_STORAGE = 0;

    protected void requestStoragePermission(View view) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Snackbar.make(view, "Write permission is required to save image to gallery", Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_STORAGE);
                        }
                    }).show();
        } else {
            Toast.makeText(getApplicationContext(), "Permission Required!", Toast.LENGTH_SHORT)
                    .show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_STORAGE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
               // chart.saveToGallery();
            } else {
                Toast.makeText(getApplicationContext(), "Saving FAILED!", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }





}

