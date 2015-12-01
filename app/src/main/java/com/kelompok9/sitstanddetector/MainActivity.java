package com.kelompok9.sitstanddetector;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.speech.tts.TextToSpeech;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private String state = null;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mProximity;

    private List<Sensor> sensorList;

    private ArrayList<AccelerometerData> recordedData;
    private ArrayList<StringBuilder> prepareString;

    private TextView xText;
    private TextView yText;
    private TextView zText;
    private TextView statusText;

    private TextToSpeech tts;

    boolean isRecording;
    int detectDataCounter = 0;
    int resettableDataCounter = 0;
    String proximityValue = "";

    final int windowSize = 10;

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
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        // Register sensor
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);

        // x y z generated display
        xText = (TextView) findViewById(R.id.xText);
        yText = (TextView) findViewById(R.id.yText);
        zText = (TextView) findViewById(R.id.zText);

        statusText = (TextView) findViewById(R.id.statusText);

        // Path to SDCard
        sdCard = Environment.getExternalStorageDirectory();
        directory = new File (sdCard.getAbsolutePath());

        recordedData = new ArrayList<AccelerometerData>();
        prepareString = new ArrayList<StringBuilder>();

        // Naive bayes
        // Forget learned data after reaching 500 data milestone
        // bayes.setMemoryCapacity(500);
        // Initial state
        state = "STOP";

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.US);
                }
            }
        });

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
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
        float x = (float) 0.0;
        float y = (float) 0.0;
        float z = (float) 0.0;

        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            x = event.values[0]; // x
            y = event.values[1]; // y
            z = event.values[2]; // z
        }

        if(event.sensor.getType() == Sensor.TYPE_PROXIMITY){
            float value = event.values[0];
            if (event.values[0] == 0.0) {
                proximityValue = "MIN";
                state = "DETECT";
            }
            else {
                proximityValue = "MAX";
                state = "STOP";
            }
        }

        if (state.equals("DETECT")) {
            if (resettableDataCounter == 50) {
                MovingAverage maX = new MovingAverage(windowSize);
                MovingAverage maY = new MovingAverage(windowSize);
                MovingAverage maZ = new MovingAverage(windowSize);
                float xSum = (float) 0.0;
                float ySum = (float) 0.0;
                float zSum = (float) 0.0;
                int qty = 0;
                for (AccelerometerData data : recordedData) {
                    maX.newNum(data.x);
                    maY.newNum(data.y);
                    maZ.newNum(data.z);

                    xSum += maX.getAvg();
                    ySum += maY.getAvg();
                    zSum += maZ.getAvg();

                    qty += 1;
                }
                float xAvg = xSum / (float) qty;
                float yAvg = ySum / (float) qty;
                float zAvg = zSum / (float) qty;

                JSONObject object = new JSONObject();
                try {
                    object.put("command", "detect");
                    object.put("x", xAvg);
                    object.put("y", yAvg);
                    object.put("z", zAvg);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                try {
                    makePostRequest(object);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                resettableDataCounter = 0;
            }
        } else {
            if (resettableDataCounter == 20) {
                MovingAverage maX = new MovingAverage(windowSize);
                MovingAverage maY = new MovingAverage(windowSize);
                MovingAverage maZ = new MovingAverage(windowSize);
                for (AccelerometerData data : recordedData) {
                    maX.newNum(data.x);
                    maY.newNum(data.y);
                    maZ.newNum(data.z);

                    StringBuilder prepare = new StringBuilder();
                    prepare.append(maX.getAvg());
                    prepare.append(" ");
                    prepare.append(maY.getAvg());
                    prepare.append(" ");
                    prepare.append(maZ.getAvg());
                    prepare.append("\n");

                    prepareString.add(prepare);

                }
                resettableDataCounter = 0;
                List<AccelerometerData> sublist = recordedData.subList(0, 9);
                sublist.clear();

                if (state.equals("START")) {
                    for (StringBuilder str : prepareString) {
                        try {
                            writeToFile("dataset.txt", str.toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    prepareString.clear();
                }
            }
        }

        resettableDataCounter += 1;

        AccelerometerData newData = new AccelerometerData(x, y, z);
        recordedData.add(newData);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

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

    private void makePostRequest(JSONObject object) throws IOException {
        HttpClient client = new DefaultHttpClient();
        HttpPost request = new HttpPost("https://sensor-knn-webservice-raizan.c9users.io/");
        String message = object.toString();
        request.setEntity(new StringEntity(message, "UTF8"));
        request.setHeader("Content-type", "application/json");
        HttpResponse response = client.execute(request);

        BufferedReader in = new BufferedReader(new InputStreamReader(
                response.getEntity().getContent()));

        String line = in.readLine();
        tts.speak(line, TextToSpeech.QUEUE_FLUSH, null);
    }

    private void makeGetRequest() {
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet("https://sensor-knn-webservice-raizan.c9users.io/");
        // replace with your url

        HttpResponse response;
        try {
            response = client.execute(request);

            BufferedReader in = new BufferedReader(new InputStreamReader(
                    response.getEntity().getContent()));

            String line = in.readLine();

            Toast toast = Toast.makeText(getApplicationContext(), line, Toast.LENGTH_SHORT);
            toast.show();

            Log.d("Response of GET request", response.toString());
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
