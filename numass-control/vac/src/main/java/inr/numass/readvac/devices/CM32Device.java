/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.devices;

import hep.dataforge.control.devices.PortSensor;
import hep.dataforge.control.measurements.Measurement;
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
public class CM32Device extends PortSensor<Double> {

    public CM32Device(String portName) {
        super(portName);
    }

    @Override
    protected PortHandler buildHandler(String portName) throws ControlException {
        String port = meta().getString("port", portName);
        PortHandler newHandler = new ComPortHandler(port, 2400, 8, 1, 0);
        newHandler.setDelimeter("T--");
        return newHandler;
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


    private class CMVacMeasurement extends SimpleMeasurement<Double> {

        private static final String CM32_QUERY = "MES R PM 1\r\n";

        @Override
        protected synchronized Double doMeasure() throws Exception {

            String answer = getHandler().sendAndWait(CM32_QUERY, timeout());

            if (answer.isEmpty()) {
                this.onProgressUpdate("No signal");
                updateState(CONNECTION_STATE, false);
                return null;
            } else if (answer.indexOf("PM1:mbar") < -1) {
                this.onProgressUpdate("Wrong answer: " + answer);
                updateState(CONNECTION_STATE, false);
                return null;
            } else if (answer.substring(14, 17).equals("OFF")) {
                this.onProgressUpdate("Off");
                updateState(CONNECTION_STATE, true);
                return null;
            } else {
                this.onProgressUpdate("OK");
                updateState(CONNECTION_STATE, true);
                return Double.parseDouble(answer.substring(14, 17) + answer.substring(19, 23));
            }
        }
    }

}
