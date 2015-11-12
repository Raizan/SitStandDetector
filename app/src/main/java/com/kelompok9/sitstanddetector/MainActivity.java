package com.kelompok9.sitstanddetector;

import android.content.Context;
import android.content.ContextWrapper;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import de.daslaboratorium.machinelearning.BayesClassifier;
import de.daslaboratorium.machinelearning.Classification;
import de.daslaboratorium.machinelearning.Classifier;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private String state = null;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private List<Sensor> sensorList;

    private ArrayList<AccelerometerData> recordedData;

    private TextView xText;
    private TextView yText;
    private TextView zText;
    private TextView statusText;

    boolean isRecording;
    int globalDataCounter = 0;
    int resettableDataCounter = 0;

    File sdCard;
    File directory;

    Classifier<String, String> bayes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        isRecording = false;

        // Sensor
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // Register sensor
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);


        // x y z generated display
        xText = (TextView) findViewById(R.id.xText);
        yText = (TextView) findViewById(R.id.yText);
        zText = (TextView) findViewById(R.id.zText);

        statusText = (TextView) findViewById(R.id.statusText);

        // Path to SDCard
        sdCard = Environment.getExternalStorageDirectory();
        directory = new File (sdCard.getAbsolutePath());

        recordedData = new ArrayList<AccelerometerData>();

        // Naive bayes
        bayes = new BayesClassifier<String, String>();
        // Forget learned data after reaching 500 data milestone
        // bayes.setMemoryCapacity(500);
        // Initial state
        state = "STOP";
    }

    public void onToggleBtnClicked(View v) throws IOException {
        TextView toggleBtn = (TextView) findViewById(R.id.toggleBtn);
        isRecording = !isRecording;
        if (isRecording) {
            state = "START";
            toggleBtn.setText("Stop");
            writeToFile("dataset.txt", "START\n");
//            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        }
        else {
            state = "STOP";
            toggleBtn.setText("Start");
//            mSensorManager.unregisterListener(this);
        }
    }

    public void onBerdiriBtnClicked(View v) throws IOException {
        TextView berdiriBtn = (TextView) findViewById(R.id.berdiriBtn);
        if (state.equals("STOP")) {
            Toast toast = Toast.makeText(getApplicationContext(), "BERDIRI", Toast.LENGTH_SHORT);
            toast.show();
            writeToFile("dataset.txt", "BERDIRI\n");
        }
    }

    public void onDudukBtnClicked(View v) throws IOException {
        TextView dudukBtn = (TextView) findViewById(R.id.dudukBtn);
        if (state.equals("STOP")) {
            Toast toast = Toast.makeText(getApplicationContext(), "DUDUK", Toast.LENGTH_SHORT);
            toast.show();
            writeToFile("dataset.txt", "DUDUK\n");
        }
    }

    public void onLabelBtnClicked(View v) throws IOException {
        TextView labelBtn = (TextView) findViewById(R.id.labelBtn);
        Toast toast = Toast.makeText(getApplicationContext(), "labelledDataSet.txt updated", Toast.LENGTH_SHORT);
        toast.show();
        labelByUser();
    }

    public void onDetectBtnClicked(View v) throws IOException {
        TextView detectBtn = (TextView) findViewById(R.id.detectBtn);
        state = "DETECT";
    }

    public void onDetectObsBtnClicked(View v) throws IOException {
        TextView detectObsBtn = (TextView) findViewById(R.id.detectObsBtn);
        state = "DETECT-OBS";
    }

    public void onResetBtnClicked(View v) throws IOException {
        TextView resetBtn = (TextView) findViewById(R.id.resetBtn);
        reset();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Toast toast = Toast.makeText(getApplicationContext(), "Options", Toast.LENGTH_SHORT);
            toast.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // temp dulu baru bulk insert? identitas data berdiri atau duduk terdiri atas 10 row data
    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0]; // x
        float y = event.values[1]; // y
        float z = event.values[2]; // z

        xText.setText("x:\t" + x);
        yText.setText("y:\t" + y);
        zText.setText("z:\t" + z);

        if (resettableDataCounter == 20)
        {
            float xSumAvg = 0;
            float ySumAvg = 0;
            float zSumAvg = 0;
            // calculate avg
            for (AccelerometerData dt : recordedData) {
                xSumAvg += dt.x;
                ySumAvg += dt.y;
                zSumAvg += dt.z;
            }

            float xAvg = xSumAvg / (float) recordedData.size();
            float yAvg = ySumAvg / (float) recordedData.size();
            float zAvg = zSumAvg / (float) recordedData.size();

            // analyze and display on TextView Status
            String avgNormalized = normalize(xAvg, yAvg, zAvg);
            String category = "";
            if (state.equals("DETECT")) {
                category = detect(avgNormalized);
            } else if (state.equals("DETECT-OBS")) {
                String arrayString[] = avgNormalized.split("\\s");
                if (Float.valueOf(arrayString[1]) == 1.0) {
                    category = "BERDIRI";
                }
                else if (Float.valueOf(arrayString[2]) == 1.0) {
                    category = "DUDUK";
                }
            }

            Toast toast = Toast.makeText(getApplicationContext(), "Status: " + category, Toast.LENGTH_SHORT);
            toast.show();
            statusText.setText("Status: " + category);

            String prepare = avgNormalized + " " + category;
            try {
                writeToFile("labelledDataSet.txt", prepare);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // reset state
            recordedData.clear();
            resettableDataCounter = 0;
            state = "STOP";
        }

        if (state.equals("DETECT") || state.equals("DETECT-OBS")) {
            AccelerometerData recordData = new AccelerometerData(x, y, z);
            recordedData.add(recordData);

            resettableDataCounter += 1;
        }
        else if (state.equals("START")){
            // Write RAW accelerometer data to file
            String prepare = x + " " + y + " " + z + "\n";
            try {
                writeToFile("dataset.txt", prepare);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        globalDataCounter += 1;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        mSensorManager.unregisterListener(this);
//    }

    // File manipulation functions
    private void writeToFile(String fileName, String data) throws IOException {
        //Now create the file in the above directory and write the contents into it
        File file = new File(directory, fileName);
        FileOutputStream fOut = null;
        fOut = new FileOutputStream(file, true);
        OutputStreamWriter osw = new OutputStreamWriter(fOut);
        osw.append(data);
        osw.flush();
        osw.close();
    }


    private String normalize(float xAvg, float yAvg, float zAvg) {
        float maxValue = Math.max(Math.max(Math.abs(xAvg), Math.abs(yAvg)), Math.abs(zAvg));
        float xAvgNormal = Math.abs(xAvg) / maxValue;
        float yAvgNormal = Math.abs(yAvg) / maxValue;
        float zAvgNormal = Math.abs(zAvg) / maxValue;

        return xAvgNormal + " " + yAvgNormal + " " + zAvgNormal;
    }

    private void labelByUser() {
        //Get the text file
        File file = new File(directory, "dataset.txt");

        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            float x = 0;
            float y = 0;
            float z = 0;
            int quantity = 0;

            ArrayList<AccelerometerData> avgData = new ArrayList<AccelerometerData>();

            while ((line = br.readLine()) != null) {
                String arrayString[] = line.split("\\s+");
                if (arrayString[0].equals("START")) {
                    continue;
                } else if (arrayString[0].equals("NaN")) {
                    continue;
                } else if (arrayString[0].equals("BERDIRI") || arrayString[0].equals("DUDUK")) {
                    // calculate average
                    float xAvg = x / (float) quantity;
                    float yAvg = y / (float) quantity;
                    float zAvg = z / (float) quantity;
                    quantity = 0;
                    x = 0;
                    y = 0;
                    z = 0;
                    String avgNormalized = normalize(xAvg, yAvg, zAvg);
                    String prepare = avgNormalized + " " + arrayString[0] + "\n";

                    writeToFile("labelledDataSet.txt", prepare);
                } else {
                    x += Float.valueOf(arrayString[0]);
                    y += Float.valueOf(arrayString[1]);
                    z += Float.valueOf(arrayString[2]);
                    quantity += 1;
                }
            }
            br.close();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
        }
    }

    private void reset() {
        File file = new File(directory, "labelledDataSet.txt");

        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                String arrayString[] = line.split("\\s+");
                if (arrayString[0].equals("NaN")) {
                    continue;
                } else {
                    String categorized = categorize(arrayString);
                    bayes.learn(arrayString[3], Arrays.asList(categorized.split("\\s")));
                }
            }
            Toast toast = Toast.makeText(getApplicationContext(), "Bayes re-learned", Toast.LENGTH_SHORT);
            toast.show();
            br.close();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
        }
    }

    private String categorize(String[] arrayString) {
        // hi low
        StringBuilder categorized = new StringBuilder();
        float threshold = (float) 0.4;
        if (Float.valueOf(arrayString[0]) >= threshold) {
            categorized.append("x ");

        }
        if (Float.valueOf(arrayString[1]) >= threshold) {
            categorized.append("y ");
        }
        if (Float.valueOf(arrayString[2]) >= threshold) {
            categorized.append(" z");
        }
        Toast toast = Toast.makeText(getApplicationContext(), categorized.toString(), Toast.LENGTH_SHORT);
        toast.show();

        return categorized.toString();
    }

    private String detect(String avgNormalized) {
        return bayes.classify(Arrays.asList(avgNormalized.split("\\s"))).getCategory();
    }
}
