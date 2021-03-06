/*
 * Copyright 2018 Daniel Larsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codingginger.hts2212;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Sensor range
 * <li>{@link #MIN_TEMP_C}</li>
 * <li>{@link #MAX_TEMP_C}</li>
 * <li>{@link #MIN_HUM}</li>
 * <li>{@link #MAX_HUM}</li>
 *
 */
enum range
{
    /**
     * Minimum temperature for this sensor
     */
    MIN_TEMP_C("-40f"),
    /**
     * Maximum temperature for this sensor
     */
    MAX_TEMP_C("120f"),
    /**
     * Minimum humidity for this sensor
     */
    MIN_HUM("0%"),
    /**
     * Maximum humidity for this sensor
     */
    MAX_HUM("100%");

    private String value;

    range(String s) {
        value = s;
    }

    public String getRange(){
        return value;
    }
}

/**
 * Register address
 * <li>{@link #I2C_ADDRESS}</li>
 * <li>{@link #TEMP_OUT_L}</li>
 * <li>{@link #TEMP_OUT_H}</li>
 * <li>{@link #HUMIDITY_OUT_L}</li>
 * <li>{@link #HUMIDITY_OUT_H}</li>
 * <li>{@link #WHO_AM_I}</li>
 * <li>{@link #WHO_AM_I_RETURN}</li>
 * <li>{@link #REG_CTRL}</li>
 * <li>{@link #ODR0_SET}</li>
 * <li>{@link #BDU_SET}</li>
 * <li>{@link #POWER_MODE_ACTIVE}</li>
 */
enum regAddr
{
    /**
     * Chip id HTS221
     */
    I2C_ADDRESS(0x5f),
    /**
     * Temperature LSB
     */
    TEMP_OUT_L(0x2A),
    /**
     * Temperature MSB
     */
    TEMP_OUT_H(0x2B),
    /**
     * Humidity LSB
     */
    HUMIDITY_OUT_L(0x28),
    /**
     * Humidity MSB
     */
    HUMIDITY_OUT_H(0x29),
    /**
     * Device identification
     */
    WHO_AM_I(0x0f),
    /**
     * Device identifier
     */
    WHO_AM_I_RETURN(0xBC),
    /**
     * Control register 1
     */
    REG_CTRL(0x20),
    /**
     * Output data rate
     */
    ODR0_SET(0x1),
    /**
     * Block Device update
     */
    BDU_SET(0x4),
    /**
     * Power control, Active
     */
    POWER_MODE_ACTIVE(0x7);

    private int address;

    regAddr(int _address) {
        address = _address;
    }

    public int getAddress(){
        return address;
    }
}
//TODO: Change calib name to something more relevant
/**
 * Calibration
 */
enum calib
{
    _h0_rH,
    _h1_rH,
    _T0_degC,
    _T1_degC,
    _H0_T0,
    _H1_T0,
    _T0_OUT,
    _T1_OUT,
    mMode;

    private char c;
    private int i;

    public void setC(char CHAR) {
        this.c = CHAR;
    }

    public void setI(int INT) {
        this.i = INT;
    }

    public char getC() {
        return c;
    }

    public int getI() {
        return i;
    }
}
/**
 * @author Daniel Larsson <znixen@live.se> partionally based/influesed on SmartEverything ST HTS221 Humidity Sensor
 * @version 1.0
 * Driver for SenseHat Humidity and Temperature sensor, HTS221.
 */
public class HTS221 implements AutoCloseable {

    private I2cDevice mDevice;
    private static final String BUS = "I2C1";
    private static final int CALIB_START = 0x30; // Calibration start
    private static final int CALIB_END = 0x3F; // Calibration ends



    @Retention(RetentionPolicy.SOURCE)
    @interface Mode {}
    private static final int MODE_DOWN = ~0b10000000; //~0x80
    private static final int MODE_ACTIVE = 0b10000000; //0x80

    /**
     * Creates a HTS221 driver connected on the given I2C bus.
     */
    public HTS221(){
        PeripheralManager pioService = PeripheralManager.getInstance();
        I2cDevice device = null;
        try {
            device = pioService.openI2cDevice(BUS, regAddr.I2C_ADDRESS.getAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            connect(device);
        } catch (IOException e) {
            try {
                close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * @param device takes an I2CDevice as a parameter
     * */
    private void connect(I2cDevice device) throws IOException {
        mDevice = device;
        boolean on = pOn(MODE_ACTIVE);
        whoAmI(on);
        dbuOn();
    }

    /**
     * @param on is a boolean
     */
    private void whoAmI(boolean on) throws IOException {
        if (on){
            int data = mDevice.readRegByte(regAddr.WHO_AM_I.getAddress()) & 0xff;
            if (data == regAddr.WHO_AM_I_RETURN.getAddress()){
                storeCalib();
            }else{
                mDevice.readRegByte(regAddr.WHO_AM_I_RETURN.getAddress());
            }
        }else{
            System.out.println("Error: Device not on");
        }
    }

    /**
     * Asks for calibration data to be used when temperature and humidity is pulled.
     * @throws IOException
     */
    private void storeCalib() throws IOException {
        int data;
        int temp;
        int temp2;
        for (int reg = CALIB_START; reg <= CALIB_END; reg++){
            if((reg != CALIB_START+8) && (reg != CALIB_START+9) &&(reg != CALIB_START+4)){
                data = readSample(reg);
                switch (reg){
                    case CALIB_START:
                        calib._h0_rH.setC((char)data);
                        break;
                    case CALIB_START+1:
                        calib._h1_rH.setC((char)data);
                        break;
                    case CALIB_START+2:
                        calib._T0_degC.setI(data);
                        break;
                    case CALIB_START+3:
                        calib._T1_degC.setI(data);
                        break;
                    case CALIB_START+5:
                        temp = calib._T0_degC.getI();
                        temp2 = (data&0x3)<<8;
                        temp2 |= temp;
                        calib._T0_degC.setI(temp2);

                        temp = calib._T1_degC.getI();
                        temp2 = ((data&0xC)>>2)<<8;
                        temp2 |= temp;
                        calib._T1_degC.setI(temp2);
                        break;
                    case CALIB_START+6:
                        calib._H0_T0.setI(data);
                        break;
                    case CALIB_START+7:
                        temp = calib._H0_T0.getI();
                        temp |= data<<8;
                        calib._H0_T0.setI(temp);
                        break;
                    case CALIB_START+0xA:
                        calib._H1_T0.setI(data);
                        break;
                    case CALIB_START+0xB:
                        temp = calib._H1_T0.getI();
                        temp |= data <<8;
                        calib._H1_T0.setI(temp);
                        break;
                    case CALIB_START+0xC:
                        calib._T0_OUT.setI(data);
                        break;
                    case CALIB_START+0xD:
                        temp = calib._T0_OUT.getI();
                        temp |= data << 8;
                        calib._T0_OUT.setI(temp);
                        break;
                    case CALIB_START+0xE:
                        calib._T1_OUT.setI(data);
                        break;
                    case CALIB_START+0xF:
                        temp = calib._T1_OUT.getI();
                        temp |= data << 8;
                        calib._T1_OUT.setI(temp);
                        break;
                    case CALIB_START+8:
                    case CALIB_START+9:
                    case CALIB_START+4:
                        break;
                    default:
                        return;
                }
            }
        }
    }

    /**
     * Sets device in power on mode, default is power off at boot.
     * @param mode as a parameter from @Mode Interface
     * return returns a boolean
     */
    public boolean pOn(@Mode int mode) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }
        int regCtrl = mDevice.readRegByte(regAddr.REG_CTRL.getAddress()) & 0xff;
        if (mode == MODE_DOWN){
            regCtrl &= ~regAddr.POWER_MODE_ACTIVE.getAddress();
        }else{
            regCtrl |= regAddr.POWER_MODE_ACTIVE.getAddress();
            regCtrl |= regAddr.ODR0_SET.getAddress();
        }
        mDevice.writeRegByte(regAddr.REG_CTRL.getAddress(), (byte)(regCtrl));
        calib.mMode.setI(mode);
        return true;
    }

    /**
     * Setting data block update to prohibit the lower register part to be updated,
     * until the upper register part is also read.
     * @throws IOException
     */
    private void dbuOn()throws IOException{
        int data;

        data = mDevice.readRegByte(regAddr.REG_CTRL.getAddress()) & 0xff;
        data |= regAddr.BDU_SET.getAddress();
        mDevice.writeRegByte(regAddr.REG_CTRL.getAddress(), (byte)data);
    }

    /**
     * Setting data block update back to 0, this allows for continuously update on both registers.
     * @throws IOException
     */
    private void bduOff() throws IOException{
        int data;
        data = mDevice.readRegByte(regAddr.REG_CTRL.getAddress()) & 0xff;
        data &= ~regAddr.BDU_SET.getAddress();
        mDevice.writeRegByte(regAddr.REG_CTRL.getAddress(), (byte)data);
    }

    /**
     * Turning off power mode for the device.
     * @param mode as a parameter from @Mode Interface
     * @throws IOException
     */
    private void pOff(@Mode int mode) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }
        int regCtrl = mDevice.readRegByte(regAddr.REG_CTRL.getAddress()) & 0xff;
        regCtrl &= ~regAddr.POWER_MODE_ACTIVE.getAddress();
        mDevice.writeRegByte(regAddr.REG_CTRL.getAddress(), (byte)(regCtrl));
        calib.mMode.setI(mode);
    }

    /**
     * @return double
     * @throws IOException
     */
    public double getHumidity() throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }
        int data;
        int temp;
        double deg;
        double h_temp;
        data = readSample(regAddr.HUMIDITY_OUT_H.getAddress());
        temp = data << 8;  // MSB
        data = readSample(regAddr.HUMIDITY_OUT_L.getAddress());
        temp |= data;      // LSB

        // Decode Humidity
        deg = ((int)(calib._h1_rH.getC()) - (int)(calib._h0_rH.getC()))/2.0;  // remove x2 multiple

        // Calculate humidity in decimal of grade centigrades i.e. 15.0 = 150.
        h_temp = ((temp - calib._H0_T0.getI()) * deg) /
                (double)(calib._H1_T0.getI() - calib._H0_T0.getI());
        deg    = (double)((int)calib._h0_rH.getC()) / 2.0; // remove x2 multiple
        return (deg + h_temp);
    }

    /**
     * @return double
     * @throws IOException
     */
    public double getTemperature() throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }
        int data;
        int temp;
        double deg;
        double t_temp;
        data = readSample(regAddr.TEMP_OUT_H.getAddress());
        temp = data << 8;
        data = readSample(regAddr.TEMP_OUT_L.getAddress());
        temp |= data;

        deg    = (double)((calib._T1_degC.getI()) - (calib._T0_degC.getI()))/8.0;

        t_temp = ((temp - calib._T0_OUT.getI()) * deg) /
                (double)(calib._T1_OUT.getI() - calib._T0_OUT.getI());
        deg    = (double)((int)calib._T0_degC.getI()) / 8.0;

        return deg + t_temp;
    }

    /**
     * Method for reading register.
     * @param _address
     * @throws IOException
     */
    private int readSample(int _address) throws IOException {
        return mDevice.readRegByte(_address) & 0xff;
    }



    /**
     * @return String min temperature
     */
    public String getMinTempC(){
        return range.MIN_TEMP_C.getRange();
    }

    /**
     * @return String max temperature
     */

    public String getMaxTempC(){
        return range.MAX_TEMP_C.getRange();
    }

    /**
     * @return String min humidity
     */
    public String getMinHum(){
        return range.MIN_HUM.getRange();
    }

    /**
     * @return String max humidity
     */
    public String getMaxHum(){
        return range.MAX_HUM.getRange();
    }

    /**
     * @return int mMode
     */
    public int getMode(){
        return calib.mMode.getI();
    }

    /**
     * Close method
     */
    @Override
    public void close(){
        if (mDevice != null){
            try {
                bduOff();
                pOff(MODE_DOWN);
                mDevice.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mDevice = null;
            }
        }
    }





}

