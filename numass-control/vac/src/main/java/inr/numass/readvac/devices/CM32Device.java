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

/**
 *
 * @author Alexander Nozik
 */
@ValueDef(name = "port")
@ValueDef(name = "delay")
@ValueDef(name = "timeout")
public class CM32Device extends Sensor<Double> {

    private PortHandler handler;

//    public CM32Device(String name, Context context, Meta meta) {
//        super(name, context, meta);
//    }

    public void setHandler(PortHandler handler){
        this.handler = handler;
    }
    
    /**
     * @return the handler
     */
    private PortHandler getHandler() throws ControlException {
        if (handler == null || !handler.isOpen()) {
            String port = meta().getString("port");
            getLogger().info("Connecting to port {}", port);
            handler = new ComPortHandler(port, 2400, 8, 1, 0);
            handler.setDelimeter("T--");
            handler.open();
        }
        return handler;
    }

    @Override
    protected Measurement<Double> createMeasurement() {
        return new CMVacMeasurement();
    }

    @Override
    public String type() {
        return meta().getString("type", "Leibold CM32");
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

    public boolean isConnected() {
        return getState("connection").booleanValue();
    }

    private int timeout() {
        return meta().getInt("timeout", 400);
    }
    
    private class CMVacMeasurement extends SimpleMeasurement<Double> {

        private static final String CM32_QUERY = "MES R PM 1\r\n";

        @Override
        protected synchronized Double doMeasure() throws Exception {

            String answer = handler.sendAndWait(CM32_QUERY, timeout());

            if (answer.isEmpty()) {
                this.onProgressUpdate("No signal");
                updateState("connection", false);
                return null;
            } else if (answer.indexOf("PM1:mbar") < -1) {
                this.onProgressUpdate("Wrong answer: " + answer);
                updateState("connection", false);
                return null;
            } else if (answer.substring(14, 17).equals("OFF")) {
                this.onProgressUpdate("Off");
                updateState("connection", true);
                return null;
            } else {
                this.onProgressUpdate("OK");
                updateState("connection", true);
                return Double.parseDouble(answer.substring(14, 17) + answer.substring(19, 23));
            }
        }
    }

}
