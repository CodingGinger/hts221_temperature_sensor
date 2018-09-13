package com.codingginger.hts2212;

import android.hardware.Sensor;

import com.google.android.things.userdriver.UserDriverManager;
import com.google.android.things.userdriver.sensor.UserSensor;
import com.google.android.things.userdriver.sensor.UserSensorDriver;
import com.google.android.things.userdriver.sensor.UserSensorReading;

import java.io.IOException;
import java.util.UUID;

public class HTS221SensorDriver {

    private static final String DRIVER_VENDOR = "ST";
    private static final String DRIVER_NAME = "HTS221";

    private HTS221 mDevice;

    private TemperatureUserDriver mTemperatureUserDriver;
    private HumidityUserDriver mHumidityUserDriver;

    public HTS221SensorDriver(){
        mDevice = new HTS221();
    }

    private class TemperatureUserDriver implements UserSensorDriver {
        private final float DRIVER_MAX_RANGE = Float.parseFloat(range.MAX_TEMP_C.getRange());

        private boolean mEnabled;
        private UserSensor mUserSensor;

        private UserSensor getUserSensor(){
            if (mUserSensor == null){
                mUserSensor = new UserSensor.Builder()
                        .setType(Sensor.TYPE_AMBIENT_TEMPERATURE)
                        .setName(DRIVER_NAME)
                        .setVendor(DRIVER_VENDOR)
                        .setMaxRange(DRIVER_MAX_RANGE)
                        .setUuid(UUID.randomUUID())
                        .setDriver(this)
                        .build();
            }
            return mUserSensor;
        }

        @Override
        public void setEnabled(boolean enabled) {
            mEnabled = enabled;
        }

        private boolean isEnabled() {
            return mEnabled;
        }

        @Override
        public UserSensorReading read() throws IOException {
            return new UserSensorReading(new float[]{(float)mDevice.getTemperature()});
        }
    }

    private class HumidityUserDriver implements UserSensorDriver {
        private final float DRIVER_MAX_RANGE = Float.parseFloat(range.MAX_HUM.getRange());

        private boolean mEnabled;
        private UserSensor mUserSensor;

        private UserSensor getUserSensor(){
            if (mUserSensor == null){
                mUserSensor = new UserSensor.Builder()
                        .setType(Sensor.TYPE_RELATIVE_HUMIDITY)
                        .setName(DRIVER_NAME)
                        .setVendor(DRIVER_VENDOR)
                        .setMaxRange(DRIVER_MAX_RANGE)
                        .setUuid(UUID.randomUUID())
                        .setDriver(this)
                        .build();
            }
            return mUserSensor;
        }

        @Override
        public void setEnabled(boolean enabled) {
            mEnabled = enabled;
        }

        private boolean isEnabled() {
            return mEnabled;
        }

        @Override
        public UserSensorReading read() throws IOException {
            return new UserSensorReading(new float[]{(float)mDevice.getHumidity()});
        }
    }

    private void registerTemperatureSensor(){
        if (mDevice == null){
            throw new IllegalStateException("cannot register closed driver");
        }

        if (mTemperatureUserDriver == null){
            mTemperatureUserDriver = new TemperatureUserDriver();
            UserDriverManager.getInstance().registerSensor(mTemperatureUserDriver.getUserSensor());
        }
    }

    public void unregisterTemperatureSensor() {
        if (mTemperatureUserDriver != null) {
            UserDriverManager.getInstance().unregisterSensor(mTemperatureUserDriver.getUserSensor());
            mTemperatureUserDriver = null;
        }
    }

    private void registerHumiditySensor(){
        if (mDevice == null){
            throw new IllegalStateException("cannot register closed driver");
        }

        if (mHumidityUserDriver == null){
            mHumidityUserDriver = new HumidityUserDriver();
            UserDriverManager.getInstance().registerSensor(mHumidityUserDriver.getUserSensor());
        }
    }

    public void unregisterHumiditySensor() {
        if (mHumidityUserDriver != null) {
            UserDriverManager.getInstance().unregisterSensor(mHumidityUserDriver.getUserSensor());
            mHumidityUserDriver = null;
        }
    }

}
