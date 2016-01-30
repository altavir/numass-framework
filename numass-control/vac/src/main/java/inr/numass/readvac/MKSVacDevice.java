/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac;

import hep.dataforge.context.Context;
import hep.dataforge.control.measurements.RegularMeasurement;
import hep.dataforge.control.measurements.SingleMeasurementDevice;
import hep.dataforge.control.ports.PortHandler;
import hep.dataforge.description.ValueDef;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.meta.Meta;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Alexander Nozik
 */
@ValueDef(name = "address")
@ValueDef(name = "channel")
@ValueDef(name = "port")
@ValueDef(name = "delay")
public class MKSVacDevice extends SingleMeasurementDevice<RegularMeasurement<Double>> {

//    private static final String DELIMETER = ";FF";
    private PortHandler handler;

    public MKSVacDevice(String name, Context context, Meta meta) {
        super(name, context, meta);
    }

    private String talk(String requestContent) throws ControlException {
        String answer = handler.sendAndWait(String.format("@%s%s;FF", getDeviceAddress(), requestContent), timeout());

        Matcher match = Pattern.compile("@253ACK(.*);FF").matcher(answer);
        if (match.matches()) {
            return match.group(1);
        } else {
            throw new ControlException(answer);
        }
    }

    private String getDeviceAddress() {
        //PENDING cache this?
        return meta().getString("address", "253");
    }

    private int timeout() {
        return meta().getInt("timeout", 400);
    }

    @Override
    protected RegularMeasurement<Double> createMeasurement() {
        return new MKSVacMeasurement();
    }

    @Override
    protected Object calculateState(String stateName) throws ControlException {
        if (handler == null) {
            notifyError("No port connection", null);
            return null;
        }
        switch (stateName) {
            case "connection":
                return !talk("T?").isEmpty();
            case "power":
                return talk("FP?").equals("ON");
            default:
                notifyError("State not found: " + stateName, null);
                return null;
        }
    }

    private Double readPressure(int channel) {
        try {
            String answer = talk("PR" + channel + "?");
            if (answer == null || answer.isEmpty()) {
                invalidateState("connection");
                return null;
            }
            double res = Double.parseDouble(answer);
            if (res <= 0) {
                return null;
            } else {
                return res;
            }
        } catch (ControlException ex) {
            invalidateState("connection");
            return null;
        }
    }

    public boolean isConnected() {
        return getState("connection").booleanValue();
    }

    public boolean isPowerOn() {
        return getState("power").booleanValue();
    }

    @Override
    public String type() {
        return meta().getString("type", "MKS vacuumeter");
    }

    private class MKSVacMeasurement extends RegularMeasurement<Double> {

        @Override
        protected Double doMeasurement() throws Exception {
            String answer = talk("PR" + getChannel() + "?");
            if (answer == null || answer.isEmpty()) {
                invalidateState("connection");
                this.progressUpdate("No connection");
                return null;
            }
            double res = Double.parseDouble(answer);
            if (res <= 0) {
                this.progressUpdate("Non positive");
                return null;
            } else {
                return res;
            }
        }

        private int getChannel() {
            return meta().getInt("channel", 5);
        }

        @Override
        protected boolean stopOnError() {
            return false;
        }
        
        @Override
        protected Duration getDelay() {
            return Duration.of(meta().getInt("delay", 5000), ChronoUnit.MILLIS);
        }

    }
}
