package com.codingginger.hts2212;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Daniel Larsson <znixen@live.se>
 * @version 1.0
 */
public class HTS221 implements AutoCloseable {

    private I2cDevice mDevice;

    // static variables
    public static final int I2C_ADDRESS =  0x5f; // HTS221 I2C address
    public static final String BUS = "I2C1";
    private static final int WHO_AM_I = 0x0f; // 0b1111
    private static final int WHO_AM_I_RETURN = 0xBC; // 0b10111100
    // Temperature register address in hex
    private static final int TEMP_OUT_L = 0x2A;
    private static final int TEMP_OUT_H = 0x2B;
    // Humidity register address in hex
    private static final int HUMIDITY_OUT_L = 0x28;
    private static final int HUMIDITY_OUT_H = 0x29;
    private static final int REG_CTRL = 0x20;
    private static final int POWER_MODE_ACTIVE = 0x7;
    private static final int ODR0_SET = 0x1; // setting sensor spreading to 1Hz
    private static final int BDU_SET = 0x4;
    private static final float MIN_TEMP_C = -40f; // Minimum temperature for this chip
    private static final float MAX_TEMP_C = 120f; // Maximum temperature for this chip
    private static final int CALIB_START = 0x30; // Calibration start
    private static final int CALIB_END = 0x3F; // Calibration ends
    // Declare various non-static variables
    public boolean on;
    private int mMode;
    char _h0_rH, _h1_rH;
    int  _T0_degC, _T1_degC, _H0_T0, _H1_T0, _T0_OUT, _T1_OUT;

    @Retention(RetentionPolicy.SOURCE)
    //@IntDef({MODE_DOWN, MODE_ACTIVE})
    public @interface Mode {}
    public static final int MODE_DOWN = ~0b10000000; //0x80
    public static final int MODE_ACTIVE = 0b10000000; //0x80

    /**
     * @author Daniel Larsson <znixen@live.se>
     * */
    public HTS221(){
        PeripheralManager pioService = PeripheralManager.getInstance();
        I2cDevice device = null;
        try {
            device = pioService.openI2cDevice(BUS, I2C_ADDRESS);
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
     * @author Daniel Larsson <znixen@live.se>
     * @param device takes an I2CDevice as a parameter
     * */
    private void connect(I2cDevice device) throws IOException {
        mDevice = device;
        on = pOn(MODE_ACTIVE);
        dbuOn();
    }

    /**
     * @author Daniel Larsson <znixen@live.se>
     * @param on is a boolean
     */
    public void whoAmI(boolean on) throws IOException {
        if (on == true){
            System.out.println("It´s on!");
            int data = mDevice.readRegByte(WHO_AM_I) & 0xff;
            System.out.println("data            WHO_AM_I_RETURN");
            if (data == WHO_AM_I_RETURN){
                System.out.println(data + "     " + WHO_AM_I_RETURN);
                storeCalib();
            }else{
                int data2 = mDevice.readRegByte(WHO_AM_I_RETURN) & 0xff;
                System.out.println(data +  "    " + data2);
            }
        }else{
            System.out.println("Error: Device not on");
        }
    }

    /**
     * @author Daniel Larsson <znixen@live.se>
     * @return returns a boolean
     */
    public boolean storeCalib() throws IOException {
        int data;
        int temp;
        for (int reg = CALIB_START; reg <= CALIB_END; reg++){
            if((reg != CALIB_START+8) && (reg != CALIB_START+9) &&(reg != CALIB_START+4)){
                data = readSample(reg);
                switch (reg){
                    case CALIB_START:
                        _h0_rH = (char)data;
                        break;
                    case CALIB_START+1:
                        _h1_rH = (char)data;
                        break;
                    case CALIB_START+2:
                        _T0_degC = data;
                        break;
                    case CALIB_START+3:
                        _T1_degC = data;
                        break;
                    case CALIB_START+5:
                        temp = _T0_degC;
                        _T0_degC = (data&0x3)<<8;
                        _T0_degC |= temp;

                        temp = _T1_degC;
                        _T1_degC = ((data&0xC)>>2)<<8;
                        _T1_degC |= temp;
                        break;
                    case CALIB_START+6:
                        _H0_T0 = data;
                        break;
                    case CALIB_START+7:
                        _H0_T0 |= data<<8;
                        break;
                    case CALIB_START+0xA:
                        _H1_T0 = data;
                        break;
                    case CALIB_START+0xB:
                        _H1_T0 |= data <<8;
                        break;
                    case CALIB_START+0xC:
                        _T0_OUT = data;
                        break;
                    case CALIB_START+0xD:
                        _T0_OUT |= data << 8;
                        break;
                    case CALIB_START+0xE:
                        _T1_OUT = data;
                        break;
                    case CALIB_START+0xF:
                        _T1_OUT |= data << 8;
                        break;
                    case CALIB_START+8:
                    case CALIB_START+9:
                    case CALIB_START+4:
                        break;
                    default:
                        return false;
                }
            }
        }
        return true;
    }

    /**
     * @author Daniel Larsson <znixen@live.se>
     * @param mode as a parameter from @Mode Interface
     * return returns a boolean
     */
    public boolean pOn(@Mode int mode) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }
        int regCtrl = mDevice.readRegByte(REG_CTRL) & 0xff;
        if (mode == MODE_DOWN){
            regCtrl &= ~POWER_MODE_ACTIVE;
        }else{
            regCtrl |= POWER_MODE_ACTIVE;
            regCtrl |= ODR0_SET;
        }
        mDevice.writeRegByte(REG_CTRL, (byte)(regCtrl));
        mMode = mode;
        System.out.println("Powermode on");
        whoAmI(on);
        return true;
    }

    /**
     * @author Daniel Larsson <znixen@live.se>
     */
    public void dbuOn()throws IOException{
        int data;

        data = mDevice.readRegByte(REG_CTRL) & 0xff;
        data |= BDU_SET;
        mDevice.writeRegByte(REG_CTRL, (byte)data);
        System.out.println("BDU set to 1");
    }

    /**
     * @author Daniel Larsson <znixen@live.se>
     * @throws IOException
     */
    public void bduOff() throws IOException{
        int data;
        data = mDevice.readRegByte(REG_CTRL) & 0xff;
        data &= ~BDU_SET;
        mDevice.writeRegByte(REG_CTRL, (byte)data);
        System.out.println("BDU set to 0");
    }

    /**
     * @author Daniel Larsson <znixen@live.se>
     * @param mode as a parameter from @Mode Interface
     * @throws IOException
     */
    public void pOff(@Mode int mode) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }
        int regCtrl = mDevice.readRegByte(REG_CTRL) & 0xff;
        regCtrl &= ~POWER_MODE_ACTIVE;
        mDevice.writeRegByte(REG_CTRL, (byte)(regCtrl));
        mMode = mode;
        System.out.println("Powermode off");
    }

    /**
     * @author Daniel Larsson <znixen@live.se>
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
        data = readSample(HUMIDITY_OUT_H);
        temp = data << 8;  // MSB
        data = readSample(HUMIDITY_OUT_L);
        temp |= data;      // LSB

        // Decode Humidity
        deg = ((int)(_h1_rH) - (int)(_h0_rH))/2.0;  // remove x2 multiple

        // Calculate humidity in decimal of grade centigrades i.e. 15.0 = 150.
        h_temp = ((temp - _H0_T0) * deg) /
                (double)(_H1_T0 - _H0_T0);
        deg    = (double)((int)_h0_rH) / 2.0; // remove x2 multiple
        double _humidity = (deg + h_temp); // provide signed % measurement unit
        return _humidity;
    }

    /**
     * @aurhor Daniel Larsson <znixen@live.se>
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
        data = readSample(TEMP_OUT_H);
        temp = data << 8;
        data = readSample(TEMP_OUT_L);
        temp |= data;

        deg    = (double)((_T1_degC) - (_T0_degC))/8.0;

        t_temp = ((temp - _T0_OUT) * deg) /
                (double)(_T1_OUT - _T0_OUT);
        deg    = (double)((int)_T0_degC) / 8.0;
        double _temperature = deg + t_temp;

        return _temperature;
    }

    /**
     * @author  Daniel Larsson <znixen@live.se>
     * @param _address
     * @return
     * @throws IOException
     */

    private int readSample(int _address) throws IOException {
        int temp = mDevice.readRegByte(_address) & 0xff;
        return temp;
    }

    /**
     * @author Daniel Larsson <znixen@live.se>
     * @return
     */
    public float getMinTempC(){
        return MIN_TEMP_C;
    }

    /**
     * @author Daniel Larsson <znixen@live.se>
     * @return float max temperature
     */

    public float getMaxTempC(){
        return MAX_TEMP_C;
    }

    /**
     * @author Daniel Larsson <znixen@live.se>
     * @return int mMode
     */
    public int getMode(){
        return mMode;
    }

    /**
     * @author Daniel Larsson <znixen@live.se>
     */
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
