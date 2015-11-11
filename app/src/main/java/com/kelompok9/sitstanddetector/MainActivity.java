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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private String state = null;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private List<Sensor> sensorList;

    private ArrayList<AccelerometerData> recordedData;

    private TextView xText;
    private TextView yText;
    private TextView zText;

    boolean isRecording;
    int globalDataCounter = 0;

    File sdCard;
    File directory;

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

        // x y z generated display
        xText = (TextView) findViewById(R.id.xText);
        yText = (TextView) findViewById(R.id.yText);
        zText = (TextView) findViewById(R.id.zText);

//        TextView sensorListTV = (TextView) findViewById(R.id.sensorListTV);
//        sensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
//        for (Sensor sensor : sensorList) {
//            sensorListTV.append(sensor.getName());
//        }

        // Path to SDCard
        sdCard = Environment.getExternalStorageDirectory();
        directory = new File (sdCard.getAbsolutePath());

        recordedData = new ArrayList<AccelerometerData>();
    }

    public void onToggleBtnClicked(View v) throws IOException {
        TextView toggleBtn = (TextView) findViewById(R.id.toggleBtn);
        isRecording = !isRecording;
        if (isRecording) {
            state = "START";
            toggleBtn.setText("Stop");
            writeToFile("dataset.txt", "START\n");
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        }
        else {
            state = "STOP";
            toggleBtn.setText("Start");
            mSensorManager.unregisterListener(this);
        }
    }

    public void onBerdiriBtnClicked(View v) throws IOException {
        TextView berdiriBtn = (TextView) findViewById(R.id.berdiriBtn);
        if (state == "STOP") {
            Toast toast = Toast.makeText(getApplicationContext(), "BERDIRI", Toast.LENGTH_SHORT);
            toast.show();
            writeToFile("dataset.txt", "BERDIRI\n");
        }
    }

    public void onDudukBtnClicked(View v) throws IOException {
        TextView dudukBtn = (TextView) findViewById(R.id.dudukBtn);
        if (state == "STOP") {
            Toast toast = Toast.makeText(getApplicationContext(), "DUDUK", Toast.LENGTH_SHORT);
            toast.show();
            writeToFile("dataset.txt", "DUDUK\n");
        }
    }

    public void onReadBtnClicked(View v){
        TextView readBtn = (TextView) findViewById(R.id.readBtn);
        labelByUser();
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

        AccelerometerData setData = new AccelerometerData(x, y, z);
        recordedData.add(setData);

        if (globalDataCounter % 10 == 0) {
            float xSum = 0;
            float ySum = 0;
            float zSum = 0;
            int quantity = 0;
            for (AccelerometerData data : recordedData) {
                xSum += data.x;
                ySum += data.y;
                zSum += data.z;
                quantity += 1;
            }
            float xAvg = xSum / (float) quantity;
            float yAvg = ySum / (float) quantity;
            float zAvg = zSum / (float) quantity;


            String prepare = xAvg + " " + yAvg + " " + zAvg + "\n";
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

    private void labelByUser() {
        //Get the text file
        File file = new File(directory, "dataset.txt");

        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            float xSum = 0;
            float ySum = 0;
            float zSum = 0;
            int quantity = 0;
            while ((line = br.readLine()) != null) {
                String arrayString[] = line.split("\\s+");
                if (arrayString[0].equals("START")) {
                    continue;
                } else if (arrayString[0].equals("NaN")) {
                    continue;
                } else if (arrayString[0].equals("BERDIRI") || arrayString[0].equals("DUDUK")) {
                    // calculate average
                    // writeToFile("labelledDataSet.txt", xyzSum + " " + arrayString[0])
                } else {
                    xSum += Float.valueOf(arrayString[0]);
                    ySum += Float.valueOf(arrayString[1]);
                    zSum += Float.valueOf(arrayString[2]);
                }
                quantity += 1;
            }
            br.close();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
        }
    }
}
