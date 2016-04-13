/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.devices;

import hep.dataforge.control.devices.PortSensor;
import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.control.measurements.SimpleMeasurement;
import hep.dataforge.control.ports.PortHandler;
import hep.dataforge.exceptions.ControlException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Alexander Nozik
 */
public class VITVacDevice extends PortSensor<Double> {

    public VITVacDevice(String portName) {
        super(portName);
    }

    @Override
    protected PortHandler buildHandler(String portName) throws ControlException {
        PortHandler  newHandler = super.buildHandler(portName);
        newHandler.setDelimeter("\r\n");
        return newHandler;
    }

    @Override
    protected Measurement<Double> createMeasurement() {
        return new CMVacMeasurement();
    }

    @Override
    public String type() {
        return meta().getString("type", "Vit vacuumeter");
    }

    @Override
    protected Object calculateState(String stateName) throws ControlException {
        if (getHandler() == null) {
            notifyError("No port connection", null);
            return null;
        }

        notifyError("State not found: " + stateName, null);
        return null;
        //TODO add connection check here
//        switch (stateName) {
//            case "connection":
//                return !talk("T?").isEmpty();
//            default:
//                notifyError("State not found: " + stateName, null);
//                return null;
//        }
    }


    private class CMVacMeasurement extends SimpleMeasurement<Double> {

        private static final String VIT_QUERY = ":010300000002FA\r\n";

        @Override
        protected synchronized Double doMeasure() throws Exception {

            String answer = getHandler().sendAndWait(VIT_QUERY, timeout());

            if (answer.isEmpty()) {
                this.progressUpdate("No signal");
                updateState("connection", false);
                return null;
            } else {
                Matcher match = Pattern.compile(":010304(\\w{4})(\\w{4})..\r\n").matcher(answer);

                if (match.matches()) {
                    double base = (double) (Integer.parseInt(match.group(1), 16)) / 10d;
                    int exp = Integer.parseInt(match.group(2), 16);
                    if (exp > 32766) {
                        exp = exp - 65536;
                    }
                    BigDecimal res = BigDecimal.valueOf(base * Math.pow(10, exp));
                    res = res.setScale(4, RoundingMode.CEILING);
                    this.progressUpdate("OK");
                    updateState("connection", true);
                    return res.doubleValue();
                } else {
                    this.progressUpdate("Wrong answer: " + answer);
                    updateState("connection", false);
                    return null;
                }
            }
        }
    }

}
