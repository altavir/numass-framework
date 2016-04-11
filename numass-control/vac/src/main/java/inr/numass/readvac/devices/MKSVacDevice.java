/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.devices;

import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.control.measurements.SimpleMeasurement;
import hep.dataforge.control.ports.ComPortHandler;
import hep.dataforge.control.ports.PortHandler;
import hep.dataforge.description.ValueDef;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.values.Value;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.adapter.JavaBeanBooleanPropertyBuilder;

/**
 *
 * @author Alexander Nozik
 */
@ValueDef(name = "address")
@ValueDef(name = "channel")
@ValueDef(name = "port")
@ValueDef(name = "delay")
@ValueDef(name = "timeout")
public class MKSVacDevice extends NumassVacDevice {

    public MKSVacDevice(String portName) {
        super(portName);
    }

    private String talk(String requestContent) throws ControlException {
        String answer = getHandler().sendAndWait(String.format("@%s%s;FF", getDeviceAddress(), requestContent), timeout());

        Matcher match = Pattern.compile("@" + getDeviceAddress() + "ACK(.*);FF").matcher(answer);
        if (match.matches()) {
            return match.group(1);
        } else {
            throw new ControlException(answer);
        }
    }

    @Override
    protected PortHandler buildHandler(String portName) throws ControlException {
        PortHandler handler = super.buildHandler(portName);
        handler.setDelimeter(";FF");
        return handler;
    }

    private String getDeviceAddress() {
        //PENDING cache this?
        return meta().getString("address", "253");
    }

    @Override
    protected Measurement<Double> createMeasurement() {
        return new MKSVacMeasurement();
    }

    @Override
    protected Object calculateState(String stateName) throws ControlException {
        if (getHandler() == null) {
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

    @Override
    protected boolean applyState(String stateName, Value stateValue) throws ControlException {
        switch (stateName) {
            case "power":
                boolean powerOn = stateValue.booleanValue();
                setPowerOn(powerOn);
                return powerOn == isPowerOn();
            default:
                return super.applyState(stateName, stateValue);
        }
    }

    public boolean isPowerOn() {
        return getState("power").booleanValue();
    }

    public void setPowerOn(boolean powerOn) throws ControlException {
        if (powerOn != isPowerOn()) {
            if (powerOn) {
//                String ans = talkMKS(p1Port, "@253ENC!OFF;FF");
//                if (!ans.equals("OFF")) {
//                    LoggerFactory.getLogger(getClass()).warn("The @253ENC!OFF;FF command is not working");
//                }
                String ans = talk("FP!ON");
                if (ans.equals("ON")) {
                    setState("power", true);
                } else {
                    this.notifyError("Failed to set power state", null);
                }
            } else {
                String ans = talk("FP!OFF");
                if (ans.equals("OFF")) {
                    setState("power", false);
                } else {
                    this.notifyError("Failed to set power state", null);
                }
            }
        }
    }

    public BooleanProperty powerOnProperty() {
        try {
            return new JavaBeanBooleanPropertyBuilder().bean(this)
                    .name("powerOn").getter("isPowerOn").setter("setPowerOn").build();
        } catch (NoSuchMethodException ex) {
            throw new Error(ex);
        }
    }

    @Override
    public String type() {
        return meta().getString("type", "MKS vacuumeter");
    }

    private int getChannel() {
        return meta().getInt("channel", 5);
    }

    private class MKSVacMeasurement extends SimpleMeasurement<Double> {

        @Override
        protected synchronized Double doMeasure() throws Exception {
            String answer = talk("PR" + getChannel() + "?");
            if (answer == null || answer.isEmpty()) {
                invalidateState("connection");
                this.onProgressUpdate("No connection");
                return null;
            }
            double res = Double.parseDouble(answer);
            if (res <= 0) {
                this.onProgressUpdate("Non positive");
                invalidateState("power");
                return null;
            } else {
                this.onProgressUpdate("OK");
                return res;
            }
        }

    }
}
