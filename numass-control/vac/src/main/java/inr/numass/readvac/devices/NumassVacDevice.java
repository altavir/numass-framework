/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.devices;

import hep.dataforge.control.measurements.Sensor;
import hep.dataforge.control.ports.PortFactory;
import hep.dataforge.control.ports.PortHandler;
import hep.dataforge.exceptions.ControlException;

/**
 *
 * @author darksnake
 */
public abstract class NumassVacDevice extends Sensor<Double> {

    private PortHandler handler;
    private final String portName;

    public NumassVacDevice(String portName) {
        this.portName = portName;
    }
    

    protected final void setHandler(PortHandler handler) {
        this.handler = handler;
    }

    public boolean isConnected() {
        return getState("connection").booleanValue();
    }

    protected int timeout() {
        return meta().getInt("timeout", 400);
    }

    protected PortHandler buildHandler(String portName) throws ControlException {
        getLogger().info("Connecting to port {}", portName);
        return PortFactory.buildPort(portName);
    }

    @Override
    public void shutdown() throws ControlException {
        super.shutdown();
        try {
            handler.close();
        } catch (Exception ex) {
            throw new ControlException(ex);
        }
    }

    /**
     * @return the handler
     * @throws hep.dataforge.exceptions.ControlException
     */
    protected PortHandler getHandler() throws ControlException {
        if (handler == null) {
            String port = meta().getString("port", portName);
            this.handler = buildHandler(port);
        }

        if (!handler.isOpen()) {
            handler.open();
        }

        return handler;
    }

}
