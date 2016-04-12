/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.devices;

import hep.dataforge.control.devices.PortSensor;
import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.control.measurements.Sensor;
import hep.dataforge.control.measurements.SimpleMeasurement;
import hep.dataforge.control.ports.ComPortHandler;
import hep.dataforge.control.ports.PortHandler;
import hep.dataforge.description.ValueDef;
import hep.dataforge.exceptions.ControlException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Alexander Nozik
 */
@ValueDef(name = "channel")
public class MKSBaratronDevice extends PortSensor<Double> {

    public MKSBaratronDevice(String portName) {
        super(portName);
    }

    private String talk(String request) throws ControlException {
        String answer = getHandler().sendAndWait(String.format("%s\r", request), timeout());

        Matcher match = Pattern.compile("(.*)\r").matcher(answer);
        if (match.matches()) {
            return match.group(1);
        } else {
            throw new ControlException(answer);
        }
    }

    @Override
    protected Measurement<Double> createMeasurement() {
        return new BaratronMeasurement();
    }

    @Override
    public String type() {
        return meta().getString("type", "MKS baratron");
    }

    @Override
    protected PortHandler buildHandler(String portName) throws ControlException {
        PortHandler handler = super.buildHandler(portName);
        handler.setDelimeter("\r");
        return handler;
    }
    
    private int getChannel() {
        return meta().getInt("channel", 2);
    }

    private class BaratronMeasurement extends SimpleMeasurement<Double> {

        @Override
        protected synchronized Double doMeasure() throws Exception {

            String answer = talk("AV" + getChannel());
            if (answer == null || answer.isEmpty()) {
//                invalidateState("connection");
                this.onProgressUpdate("No connection");
                return null;
            }
            double res = Double.parseDouble(answer);
            if (res <= 0) {
                this.onProgressUpdate("Non positive");
//                invalidateState("power");
                return null;
            } else {
                this.onProgressUpdate("OK");
                return res;
            }
        }

    }
}
