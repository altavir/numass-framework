package inr.numass.cryotemp;

import hep.dataforge.control.devices.PortSensor;
import hep.dataforge.control.measurements.AbstractMeasurement;
import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.exceptions.MeasurementException;
import hep.dataforge.tables.DataPoint;

/**
 * Created by darksnake on 28-Sep-16.
 */
public class PKT8 extends PortSensor<DataPoint> {

    public static final String PGA = "pga";
    public static final String SPS = "sps";
    public static final String ABUF = "abuf";

    @Override
    protected Measurement<DataPoint> createMeasurement() throws MeasurementException {
        return null;
    }

    @Override
    protected Object computeState(String stateName) throws ControlException {
        return super.computeState(stateName);
    }

    /**
     * '0' : 2,5 SPS '1' : 5 SPS '2' : 10 SPS '3' : 25 SPS '4' : 50 SPS '5' :
     * 100 SPS '6' : 500 SPS '7' : 1 kSPS '8' : 3,75 kSPS
     *
     * @param sps
     * @return
     */
    private String spsToStr(int sps) {
        switch (sps) {
            case 0:
                return "2,5 SPS";
            case 1:
                return "5 SPS";
            case 2:
                return "10 SPS";
            case 3:
                return "25 SPS";
            case 4:
                return "50 SPS";
            case 5:
                return "100 SPS";
            case 6:
                return "500 SPS";
            case 7:
                return "1 kSPS";
            case 8:
                return "3.75 kSPS";
            default:
                return "unknown value";
        }
    }

    /**
     * '0' : ± 5 В '1' : ± 2,5 В '2' : ± 1,25 В '3' : ± 0,625 В '4' : ± 312.5 мВ
     * '5' : ± 156,25 мВ '6' : ± 78,125 мВ
     *
     * @param sps
     * @return
     */
    private String pgaToStr(int sps) {
        switch (sps) {
            case 0:
                return "± 5 V";
            case 1:
                return "± 2,5 V";
            case 2:
                return "± 1,25 V";
            case 3:
                return "± 0,625 V";
            case 4:
                return "± 312.5 mV";
            case 5:
                return "± 156,25 mV";
            case 6:
                return "± 78,125 mV";
            default:
                return "unknown value";
        }
    }

    public String getSPS() {
        return spsToStr(sps);
    }

    public String getPGA() {
        return pgaToStr(pga);
    }

    public String getABUF() {
        return Integer.toString(abuf);
    }


    private class PKT8Measurement extends AbstractMeasurement<DataPoint> {

        @Override
        public void start() {

        }

        @Override
        public boolean stop(boolean force) throws MeasurementException {
            return false;
        }
    }
}
