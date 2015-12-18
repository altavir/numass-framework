/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac;

import hep.dataforge.context.Context;
import hep.dataforge.control.devices.AbstractMeasurementDevice;
import hep.dataforge.control.ports.ComPortHandler;
import hep.dataforge.control.ports.PortHandler;
import hep.dataforge.description.NodeDef;
import hep.dataforge.description.ValueDef;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.exceptions.PortException;
import hep.dataforge.meta.Meta;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jssc.SerialPortException;

/**
 *
 * @author Alexander Nozik
 */
@NodeDef(name = "mks", target = "metod::inr.numass.readvac.getQuery")
@ValueDef(name = "port")
@ValueDef(name = "delay")
public class MKSVacuumeterDevice extends AbstractMeasurementDevice<Double> implements PortHandler.PortController {

//    private static final String QUERY = "@253PR5?;FF";
    private static final String DELIMETER = ";FF";

    PortHandler handler;

    public MKSVacuumeterDevice(String name, Context context, Meta annotation) {
        super(name, context, annotation);
    }

    private int timeout() {
        return meta().getInt("timeout", 400);
    }

    private void initHandler() throws PortException {
        try {
            String com = meta().getString("port");
            handler = new ComPortHandler(com);
            handler.setDelimeter(DELIMETER);
        } catch (SerialPortException ex) {
            throw new PortException("Can't init port");
        }
    }

    /**
     * Get pressure query string. The default is "@253PR5?;FF"
     *
     * @return
     */
    @ValueDef(name = "address")
    @ValueDef(name = "channel")
    protected String getQuery() {
        String deviceAddres = meta().getString("mks.address", "253");
        String channelNumber = meta().getString("mks.channel", "5");
        return String.format("@%sPR%s?;FF", deviceAddres, channelNumber);
    }

    @Override
    protected void doStart(Meta measurement) throws ControlException {
        Meta meta = buildMeasurementLaminate(measurement);
        initHandler();
        setState("connection", checkConnection());
        if (isConnected()) {
            setState("power", getPowerState());
            int delay = meta.getInt("delay", 5000);
            executor.scheduleWithFixedDelay(() -> {
                Double val = read();
                measurementResult(null, val);
            }, 0, delay, TimeUnit.MILLISECONDS);
        } else {
            getLogger().warn("No connection for " + getName());
        }
    }

    @Override
    protected void doStop() throws ControlException {
        setPowerState(false);
        try {
            handler.close();
        } catch (Exception ex) {
            getLogger().error("Can not close the port", ex);
        }
    }

    private Double read() {
        if (handler == null) {
            setState("connection", false);
            return null;
        }
        try {
            String answer = talk(getQuery());
            if (answer == null || answer.isEmpty()) {
                setState("connection", false);
                return null;
            }
            double res = Double.parseDouble(answer);
            if (res <= 0) {
                return null;
            } else {
                return res;
            }
        } catch (ControlException ex) {
            setState("connection", false);
            return null;
        }
    }

    private String talk(String request) throws ControlException {

        String answer = handler.sendAndWait(request, timeout());

//        if (answer.isEmpty()) {
//            throw new ControlException("No answer from " + getName());
//        }
        Matcher match = Pattern.compile("@253ACK(.*);FF").matcher(answer);
        if (match.matches()) {
            return match.group(1);
        } else {
            throw new ControlException(answer);
        }
    }

    private boolean getPowerState() throws ControlException {
        String answer = talk("@253FP?;FF");
        return answer.equals("ON");
    }

    private boolean checkConnection() {
        try {
            return !talk("@253T?;FF").isEmpty();
        } catch (ControlException ex) {
            return false;
        }
    }

    public boolean isConnected() {
        return getState("connection").booleanValue();
    }

    public boolean isPowerOn() {
        return getState("power").booleanValue();
    }

    /**
     * Set cathode power state and return result
     *
     * @param state
     * @return
     * @throws ControlException
     */
    public void setPowerState(boolean state) throws ControlException {
        boolean powerState = getPowerState();

        if (state != powerState) {
            if (state == true) {
                String ans = talk("@253ENC!OFF;FF");
                if (!ans.equals("OFF")) {
                    getLogger().warn("The @253ENC!OFF;FF command is not working");
                }
                ans = talk("@253FP!ON;FF");
                if (!ans.equals("ON")) {
                    throw new ControlException("Can't set cathod power state");
                }
            } else {
                String ans = talk("@253FP!OFF;FF");
                if (!ans.equals("OFF")) {
                    throw new ControlException("Can't set cathod power state");
                }
            }
            setState("power", getPowerState());
        }
    }

    @Override
    public void accept(String message) {
        //ignore async responses
    }

    @Override
    public void error(String errorMessage, Throwable error) {
        getLogger().error(errorMessage, error);
    }

}
