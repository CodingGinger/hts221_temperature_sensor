package com.codingginger.hts2212;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;



enum range
{
    MIN_TEMP_C("-40f"),
    MAX_TEMP_C("120f"),
    MIN_HUM("0%"),
    MAX_HUM("100%");

    private String value;

    range(String s) {
        value = s;
    }

    public String getRange(){
        return value;
    }
}

enum regAddr
{
    I2C_ADDRESS(0x5f),
    TEMP_OUT_L(0x2A),
    TEMP_OUT_H(0x2B),
    HUMIDITY_OUT_L(0x28),
    HUMIDITY_OUT_H(0x29),
    WHO_AM_I(0x0f),
    WHO_AM_I_RETURN(0xBC),
    REG_CTRL(0x20),
    ODR0_SET(0x1),
    BDU_SET(0x4),
    POWER_MODE_ACTIVE(0x7);

    private int address;

    regAddr(int _address) {
        address = _address;
    }

    public int getAddress(){
        return address;
    }
}

enum calib
{
    _h0_rH,
    _h1_rH,
    _T0_degC,
    _T1_degC,
    _H0_T0,
    _H1_T0,
    _T0_OUT,
    _T1_OUT;

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
 * @author Daniel Larsson <znixen@live.se> partionally based/influesed on Pololu Arduino library for Pololu LPS25H and LPS331AP boards
 * @version 1.0
 */
public class HTS221 implements AutoCloseable {

    private I2cDevice mDevice;
    private static final String BUS = "I2C1";
    private static final int CALIB_START = 0x30; // Calibration start
    private static final int CALIB_END = 0x3F; // Calibration ends
    private int mMode;
    //private char _h0_rH, _h1_rH;
    //private int  _T0_degC, _T1_degC, _H0_T0, _H1_T0, _T0_OUT, _T1_OUT;

    @Retention(RetentionPolicy.SOURCE)
            //@IntDef({MODE_DOWN, MODE_ACTIVE})
    @interface Mode {}
    private static final int MODE_DOWN = ~0b10000000; //~0x80
    private static final int MODE_ACTIVE = 0b10000000; //0x80

    HTS221(){
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
        mMode = mode;
        return true;
    }

    private void dbuOn()throws IOException{
        int data;

        data = mDevice.readRegByte(regAddr.REG_CTRL.getAddress()) & 0xff;
        data |= regAddr.BDU_SET.getAddress();
        mDevice.writeRegByte(regAddr.REG_CTRL.getAddress(), (byte)data);
    }

    /**
     * @throws IOException
     */
    private void bduOff() throws IOException{
        int data;
        data = mDevice.readRegByte(regAddr.REG_CTRL.getAddress()) & 0xff;
        data &= ~regAddr.BDU_SET.getAddress();
        mDevice.writeRegByte(regAddr.REG_CTRL.getAddress(), (byte)data);
    }

    /**
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
        mMode = mode;
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
        return mMode;
    }

    @Override
    public void close(){
        if (mDevice != null){
            try {
                mDevice.close();
                bduOff();
                pOff(MODE_DOWN);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mDevice = null;
            }
        }
    }
}