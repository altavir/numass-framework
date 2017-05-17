/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac;

import hep.dataforge.context.Context;
import hep.dataforge.control.devices.PortSensor;
import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.control.measurements.SimpleMeasurement;
import hep.dataforge.control.ports.ComPortHandler;
import hep.dataforge.control.ports.PortFactory;
import hep.dataforge.control.ports.PortHandler;
import hep.dataforge.description.ValueDef;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.meta.Meta;

/**
 * @author Alexander Nozik
 */
@ValueDef(name = "port")
@ValueDef(name = "delay")
@ValueDef(name = "timeout")
public class CM32Device extends PortSensor<Double> {
    public CM32Device() {
    }

    public CM32Device(Context context, Meta meta) {
        setContext(context);
        setMetaBase(meta);
    }

    @Override
    protected PortHandler buildHandler(String portName) throws ControlException {
        getLogger().info("Connecting to port {}", portName);
        PortHandler newHandler;
        if (portName.startsWith("com")) {
            newHandler = new ComPortHandler(portName, 2400, 8, 1, 0);
        } else {
            newHandler = PortFactory.getPort(portName);
        }
        newHandler.setDelimeter("T--\r");
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

    //    @Override
//    protected int timeout() {
//        return meta().getInt("timeout", 1000);
//    }
    @Override
    protected Object computeState(String stateName) throws ControlException {
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
                this.progressUpdate("No signal");
                updateState(CONNECTION_STATE, false);
                return null;
            } else if (answer.indexOf("PM1:mbar") < -1) {
                this.progressUpdate("Wrong answer: " + answer);
                updateState(CONNECTION_STATE, false);
                return null;
            } else if (answer.substring(14, 17).equals("OFF")) {
                this.progressUpdate("Off");
                updateState(CONNECTION_STATE, true);
                return null;
            } else {
                this.progressUpdate("OK");
                updateState(CONNECTION_STATE, true);
                return Double.parseDouble(answer.substring(14, 17) + answer.substring(19, 23));
            }
        }
    }

}