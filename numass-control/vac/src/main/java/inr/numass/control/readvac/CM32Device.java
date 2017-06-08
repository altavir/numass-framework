/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.control.readvac;

import hep.dataforge.context.Context;
import hep.dataforge.control.devices.Device;
import hep.dataforge.control.devices.PortSensor;
import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.control.measurements.SimpleMeasurement;
import hep.dataforge.control.ports.ComPortHandler;
import hep.dataforge.control.ports.PortFactory;
import hep.dataforge.control.ports.PortHandler;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.meta.Meta;

/**
 * @author Alexander Nozik
 */
public class CM32Device extends PortSensor<Double> {
    public CM32Device() {
    }

    public CM32Device(Context context, Meta meta) {
        setContext(context);
        setMeta(meta);
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
        newHandler.setDelimiter("T--\r");
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

    private class CMVacMeasurement extends SimpleMeasurement<Double> {

        private static final String CM32_QUERY = "MES R PM 1\r\n";

        @Override
        protected synchronized Double doMeasure() throws Exception {

            String answer = sendAndWait(CM32_QUERY, timeout());

            if (answer.isEmpty()) {
                this.progressUpdate("No signal");
                updateState(CONNECTED_STATE, false);
                return null;
            } else if (!answer.contains("PM1:mbar")) {
                this.progressUpdate("Wrong answer: " + answer);
                updateState(CONNECTED_STATE, false);
                return null;
            } else if (answer.substring(14, 17).equals("OFF")) {
                this.progressUpdate("Off");
                updateState(CONNECTED_STATE, true);
                return null;
            } else {
                this.progressUpdate("OK");
                updateState(CONNECTED_STATE, true);
                return Double.parseDouble(answer.substring(14, 17) + answer.substring(19, 23));
            }
        }

        @Override
        public Device getDevice() {
            return CM32Device.this;
        }
    }

}
