package com.example.iperfv2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.provider.Settings;
import android.system.ErrnoException;
import android.system.Os;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;

import com.github.mikephil.charting.data.LineData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.*;

public class MainActivity extends AppCompatActivity implements PresetAdapter.ListItemClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    // Used to rotate main view and preset view
    private ViewFlipper flipper;

    // Main view
    private RecyclerView mRecycler;
    private EditText inputText;
    private ToggleButton toggle;
    private ChartHandler chart;
    private TestAdapter testAdapter;
    private TestHandler testHandler;

    // Preset view
    private RecyclerView pRecycler;
    private PresetHandler presetHandler;
    private PresetAdapter presetAdapter;

    // Misc containers and services
    private TestTask task;
    private ExecutorService executorService;
    private StringBuilder builder;
    private ArrayList<String> presets;
    private ArrayList<Float> pair;
    private float interval;
    private boolean done;

    public static int PICK_FILE = 1;
    public static int PICK_PRESET = 2;

    // Creates activity and initiates all views and instances used.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // Necessary in order for iPerf to create test files containing data to transfer.
            Os.setenv("TMPDIR", "/data/data/com.example.iperfv2/cache/", true);
        } catch (ErrnoException e) {
            e.printStackTrace();
        }
        setContentView(R.layout.activity_main);
        initViews();
    }

    // Creates and inflates menu bar, specified in res/menu/menu.xml
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    // Fills menu bar options with methods
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_help:

                AlertDialog alertDialog = new AlertDialog.Builder(this).create(); //Read Update
                alertDialog.setTitle("Help");
                alertDialog.setMessage("" +
                        "1. Give file access in order to save and import tests and presets \n\n" +
                        "2. Import presets from txt file or input your own command \n\n" +
                        "3. Run test and save or clear dialog \n\n " +
                        "By default '-f m' and '--forceflush' is added if not found in iperf cmd");
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
            case R.id.menu_import:
                Intent intentImport = new Intent(Intent.ACTION_GET_CONTENT);
                intentImport.setType("text/plain");
                startActivityForResult(intentImport, PICK_FILE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Initiates views, handlers and containers
    public void initViews() {
        flipper = findViewById(R.id.myViewFlipper);
        builder = new StringBuilder();
        presets = new ArrayList<>();
        pair = new ArrayList<>();

        chart = new ChartHandler(MainActivity.this, this);

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

        //On off process button
        toggle = (ToggleButton) findViewById(R.id.toggleButton);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //On
                if (isChecked) {
                    View view = MainActivity.this.getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                    clearView();
                    String cmd = inputText.getText().toString();
                    String result = formatCmd(cmd);
                    if (result != null && !result.isEmpty()) {
                        executeTest(result);
                    } else {
                        toggle.setChecked(false);
                    }
                    //Off
                } else {
                    if (task.getProcess() != null) {
                        task.getProcess().destroy();
                    }
                }
            }
        });
        // Load presets button handler
        Button button = (Button) findViewById(R.id.loadButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intentPresets = new Intent(Intent.ACTION_GET_CONTENT);
                intentPresets.setType("text/plain");
                startActivityForResult(intentPresets, PICK_PRESET);
            }
        });
    }

    // //   //  //  //  //  //  //
    //  Process related methods //
    //  //  //  //  //  //  //  //
    public void executeTest(String cmd) {
        done = false;
        executorService = Executors.newSingleThreadExecutor();
        task = new TestTask(cmd, testHandler, 1, this);
        executorService.execute(new Thread(task));
    }

    //Used to destroy running proccess
    @Override
    public void onDestroy() {
        if (testHandler != null) {
            testHandler.removeCallbacksAndMessages(null);
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
        if (task.getProcess() != null) {
            task.getProcess().destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        //Prevent app from detaching processes when accidentally going back
    }

    // Listens for click on preset RecyclerView and fetches clicked string to input box and switches view
    @Override
    public void onListItemClick(int position) {

        inputText.setText(presets.get(position));
        flipper.showNext();
    }

    // //   //  //  //
    //  Misc methods //
    //  //  //  //  //
    public void clearView() {
        done = false;

        chart.getChart().clear();
        LineData data = new LineData();
        chart.getChart().setData(data);


        TestAdapter adapter = (TestAdapter) mRecycler.getAdapter();
        adapter.clear();
        mRecycler.removeAllViewsInLayout();
        builder.setLength(0);
    }

    public String formatCmd(String cmd) {
        interval = 1;
        String newCmd = cmd.trim();
        Pattern p;
        Matcher m;

        if(!cmd.startsWith("iperf3")) {
            return newCmd;
        }

        if(!cmd.contains("--forceflush")) {
            newCmd += " --forceflush";
        }
        p = Pattern.compile("(-i\\s*|--interval\\s*)(\\d*)");
        m = p.matcher(newCmd);
        if(m.find()) {
            interval = Integer.parseInt(m.group(2));
        }
        p = Pattern.compile("(-f\\s*|--format\\s*)[kmgtKMGT]");
        m = p.matcher(newCmd);
        if(!m.find()) {
            newCmd += " -fm";
        }
        return newCmd;
    }

    //Method to load presets from textfile
    public void loadPresets(Uri uri) {
        String preset;
        PresetAdapter adapter = (PresetAdapter) pRecycler.getAdapter();
        adapter.clear();
        pRecycler.removeAllViewsInLayout();
        BufferedReader reader = null;
        
        try {
            reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));

            while ((preset = reader.readLine()) != null) {

                presets.add(preset);
                Message msg = presetHandler.obtainMessage();
                msg.obj = preset + "\r\n";
                msg.what = 10;
                msg.sendToTarget();
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Saves screenshot of graph and log file to iPerf location in external storage
    public void saveTest() {
        LocalDateTime time = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd-HH.mm.ss");
        String folderName = time.format(formatter);

        String fileContents = builder.toString();

        try {
            File extBaseDir = Environment.getExternalStorageDirectory();
            File file = new File(extBaseDir.getAbsolutePath() + "/iPerf/" + folderName );
            file.mkdirs();

            String filePath = file.getAbsolutePath();
            FileOutputStream out = null;

            out = new FileOutputStream(filePath + "/log.txt");
            out.write(fileContents.getBytes());
            out.flush();
            out.close();

            Message msg = testHandler.obtainMessage();
            msg.obj = "Saved to " + filePath + "\r\n";
            msg.what = 10;
            msg.sendToTarget();

            chart.getChart().saveToPath("graph", "/iPerf/" + folderName);

            }catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Used
    private void readTextFile(Uri uri){
        clearView();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
            String tmpLine = "";

            interval = 1;

            while ((tmpLine = reader.readLine()) != null) {

                formatGraph(tmpLine + "\r\n");

                Message msg = testHandler.obtainMessage();
                msg.obj = tmpLine + "\r\n";
                msg.what = 10;
                msg.sendToTarget();
                }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void formatGraph(String msg) {
        Pattern p = Pattern.compile("(T|R)X-C");
        Matcher m = p.matcher(msg);

        if (msg.contains("- - - ")) {
            done = true;
        }

        if (!done) {
            if (m.find()) {
                Pattern pp = Pattern.compile("(\\d*\\.*\\d*)\\d*\\s(\\w*)bits\\/sec");
                Matcher mm = pp.matcher(msg);
                while(mm.find()) {
                    if (pair.size() == 0) {
                        pair.add(Float.valueOf(mm.group(1)));

                    } else if (pair.size() == 1) {
                        pair.add(Float.valueOf(mm.group(1)));
                        chart.addDualEntry(pair.get(0), pair.get(1), interval);
                        pair.clear();
                    }
                }
            } else {
                Pattern pp = Pattern.compile("(\\d*\\.*\\d*)\\d*\\s(\\w*)bits\\/sec");
                Matcher mm = pp.matcher(msg);
                while (mm.find()) {
                    float value = Float.valueOf(mm.group(1));
                    chart.addEntry(value, interval);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE) {
            if (resultCode == RESULT_OK) {
                // User pick the file
                Uri uri = data.getData();
                readTextFile(uri);
                Toast.makeText(this, "Test imported!", Toast.LENGTH_LONG).show();
            } else {
                System.out.print("testLoad");
            }
        }else if (requestCode == PICK_PRESET) {
            if (resultCode == RESULT_OK) {
                // User pick the file
                Uri uri = data.getData();
                loadPresets(uri);
                Toast.makeText(this, "Presets loaded!", Toast.LENGTH_LONG).show();
            } else {
                System.out.print("testPrint");
            }
        }
    }

    public RecyclerView getmRecycler() {
        return mRecycler;
    }

    public RecyclerView getpRecycler() {
        return pRecycler;
    }

    public TestAdapter getTestAdapter() {
        return testAdapter;
    }

    public PresetAdapter getPresetAdapter() {
        return presetAdapter;
    }

    public ToggleButton getToggle() {
        return toggle;
    }

    public StringBuilder getBuilder() {
        return builder;
    }
}

