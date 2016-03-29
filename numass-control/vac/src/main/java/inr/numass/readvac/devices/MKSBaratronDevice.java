/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.devices;

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
@ValueDef(name = "address")
@ValueDef(name = "channel")
@ValueDef(name = "port")
@ValueDef(name = "delay")
@ValueDef(name = "timeout")
public class MKSBaratronDevice extends Sensor<Double> {

    private PortHandler handler;

    public void setHandler(PortHandler handler) {
        this.handler = handler;
    }

    private String talk(String request) throws ControlException {
        String answer = getHandler().sendAndWait(String.format("@%s%s\r", getDeviceAddress(), request), timeout());

        Matcher match = Pattern.compile("(.*)\r").matcher(answer);
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
    protected Measurement<Double> createMeasurement() {
        return new BaratronMeasurement();
    }


    public boolean isConnected() {
        return getState("connection").booleanValue();
    }

    @Override
    public String type() {
        return meta().getString("type", "MKS baratron");
    }

    /**
     * @return the handler
     */
    private PortHandler getHandler() throws ControlException {
        if (handler == null || !handler.isOpen()) {
            String port = meta().getString("port");
            getLogger().info("Connecting to port {}", port);
//            handler = PortFactory.buildPort(port);
            handler = new ComPortHandler(port);
            handler.setDelimeter("\r");
            handler.open();
        }
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
