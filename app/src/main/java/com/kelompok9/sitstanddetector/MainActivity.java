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
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

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

    public void onToggleBtnClicked(View v) {
        TextView toggleBtn = (TextView) findViewById(R.id.toggleBtn);
        if (isRecording) {
            toggleBtn.setText("Stop");
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        }
        else {
            toggleBtn.setText("Start");
            mSensorManager.unregisterListener(this);
        }
        isRecording = !isRecording;
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(globalDataCounter % 10 == 0)
        {
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

            String xyzAvg = xAvg + " " + yAvg + " " + zAvg + "\n";
            try {
                writeToFile(xyzAvg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        float x = event.values[0]; // x
        float y = event.values[1]; // y
        float z = event.values[2]; // z

        AccelerometerData data = new AccelerometerData(x, y, z);
        recordedData.add(data);

        xText.setText("x:\t" + x);
        yText.setText("y:\t" + y);
        zText.setText("z:\t" + z);

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
    private void writeToFile(String data) throws IOException {
        //Now create the file in the above directory and write the contents into it
        File file = new File(directory, "dataset.txt");
        FileOutputStream fOut = null;
        fOut = new FileOutputStream(file, true);
        OutputStreamWriter osw = new OutputStreamWriter(fOut);
        osw.append(data);
        osw.flush();
        osw.close();
    }

    private void average() {

    }

}
