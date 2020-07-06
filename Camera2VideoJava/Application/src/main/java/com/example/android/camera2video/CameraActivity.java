/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.camera2video;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;

import java.io.File;

public class CameraActivity extends Activity {

    private SensorManager mSensorManager;

    private Sensor mGyroSensor = null;
    private Sensor mAccelerometer = null;

    private static double gyroX;
    private static double gyroY;
    private static double gyroZ;

    private  static double accX;
    private  static double accY;
    private  static double accZ;
    private static int count;
    private static SharedPreferences sf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        String dir_path = Environment.getExternalStorageDirectory() + "/CaptureApp";

        if (!dir_exists(dir_path)){
            File directory = new File(dir_path);
            directory.mkdirs();
        }

        dir_path = Environment.getExternalStorageDirectory() + "/LogFile";

        if (!dir_exists(dir_path)){
            File directory = new File(dir_path);
            directory.mkdirs();
        }

        setContentView(R.layout.activity_camera);
        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, Camera2VideoFragment.newInstance())
                    .commit();
        }

        sf = getPreferences(Context.MODE_PRIVATE);
        count = sf.getInt("count",0);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public boolean dir_exists(String dir_path)
    {
        boolean ret = false;
        File dir = new File(dir_path);
        if(dir.exists() && dir.isDirectory())
            ret = true;
        return ret;
    }

    public static int getCount(){
        count = sf.getInt("count",0);
        return count;
    }

    public static SharedPreferences getSF(){
        return sf;
    }

    public static int addCount(){
        count+=1;
        SharedPreferences.Editor editor = sf.edit();
        editor.putInt("count",count);
        editor.commit();
        return count;
    }

    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(gyroListener, mGyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(acceleroListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void onStop() {
        super.onStop();
        mSensorManager.unregisterListener(gyroListener);
        mSensorManager.unregisterListener(acceleroListener);
    }
    public SensorEventListener gyroListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor mGyroSensor, int acc) {
        }

        public void onSensorChanged(SensorEvent event) {
            gyroX = event.values[0];
            gyroY = event.values[1];
            gyroZ = event.values[2];
            //SLog.d("MOBED","Gyro: "+gyroX+ " "+gyroY+" "+gyroZ+"rad/s");
        }
    };
    public SensorEventListener acceleroListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor mGyroSensor, int acc) {
        }

        public void onSensorChanged(SensorEvent event) {
            accX = event.values[0];
            accY = event.values[1];
            accZ = event.values[2];
            //Log.d("MOBED","Accelerometer: "+accX+ " "+accY+" "+accZ+"m/s^2");
        }
    };
    public static String getGyroData(){
        return gyroX+ ", "+gyroY+", "+gyroZ;
    }
    public static String getAcceleroData(){
        return accX+ ", "+accY+", "+accZ;
    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed();

    }
}
