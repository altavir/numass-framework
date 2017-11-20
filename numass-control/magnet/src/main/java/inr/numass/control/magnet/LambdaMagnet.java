/* 
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package inr.numass.control.magnet;

import hep.dataforge.context.Context;
import hep.dataforge.control.devices.AbstractDevice;
import hep.dataforge.control.devices.StateDef;
import hep.dataforge.control.ports.GenericPortController;
import hep.dataforge.control.ports.PortFactory;
import hep.dataforge.control.ports.PortTimeoutException;
import hep.dataforge.description.ValueDef;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.exceptions.PortException;
import hep.dataforge.meta.Meta;
import hep.dataforge.utils.DateTimeUtils;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static hep.dataforge.values.ValueType.*;

/**
 * @author Polina
 */
@ValueDef(name = "timeout", type = {NUMBER}, def = "400", info = "A timeout for port response")
@StateDef(value = @ValueDef(name = "output", type = BOOLEAN, info = "Weather output on or off"), writable = true)
@StateDef(value = @ValueDef(name = "current", type = NUMBER, info = "Current current"))
@StateDef(value = @ValueDef(name = "voltage", type = NUMBER, info = "Current voltage"))
@StateDef(value = @ValueDef(name = "targetCurrent", type = NUMBER, info = "Target current"), writable = true)
@StateDef(value = @ValueDef(name = "targetVoltage", type = NUMBER, info = "Target voltage"), writable = true)
@StateDef(value = @ValueDef(name = "lastUpdate", type = TIME, info = "Time of the last update"), writable = true)
public class LambdaMagnet extends AbstractDevice {

    private static final DecimalFormat LAMBDA_FORMAT = new DecimalFormat("###.##");
    public static double CURRENT_PRECISION = 0.05;
    //    public static double CURRENT_STEP = 0.05;
    public static int DEFAULT_DELAY = 1;
    public static int DEFAULT_MONITOR_DELAY = 2000;
    public static double MAX_STEP_SIZE = 0.2;
    public static double MIN_UP_STEP_SIZE = 0.005;
    public static double MIN_DOWN_STEP_SIZE = 0.05;
    public static double MAX_SPEED = 5d; // 5 A per minute

    private boolean closePortOnShutDown = false;

    private final String name;
    private final int address;
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);

    protected MagnetStateListener listener;
    //    private volatile double current = 0;
    private final Duration timeout;
//    private Future monitorTask;
//    private Future updateTask;
//    private Instant lastUpdate = null;

    private double speed = MAX_SPEED;

    private final GenericPortController controller;


    /**
     * A setup for single magnet controller
     *
     * @param context
     * @param meta
     * @throws ControlException
     */
    public LambdaMagnet(Context context, Meta meta) throws ControlException {
        this(context, meta, new GenericPortController(context, PortFactory.getPort(meta.getString("port"))));
        closePortOnShutDown = true;
    }


    /**
     * Initialize magnet device with given port controller
     *
     * @param context
     * @param meta
     * @param controller
     */
    public LambdaMagnet(Context context, Meta meta, GenericPortController controller) {
        super(context, meta);
        this.controller = controller;
        name = meta.getString("name", "LAMBDA");
        address = meta.getInt("address", 1);
        timeout = meta.optString("timeout").map(Duration::parse).orElse(Duration.ofMillis(200));
    }

//    /**
//     * This method creates an element of class MegnetController with exact
//     * parameters. If you have two parameters for your method - the next
//     * constructor will be used.
//     *
//     * @param name
//     * @param port    number of COM-port on your computer that you want to use
//     * @param address number of TDK - Lambda
//     * @param timeout waiting time for response
//     */
//    public LambdaMagnet(String name, Port port, int address, int timeout) {
//        this.name = name;
//        this.port = port;
//        this.port.setDelimiter("\r");//PENDING меняем состояние внешнего объекта?
//        this.address = address;
//        this.timeout = Duration.ofMillis(timeout);
//    }
//
//    public LambdaMagnet(Port port, int address, int timeout) {
//        this(null, port, address, timeout);
//    }
//
//    public LambdaMagnet(Port port, int address) {
//        this(null, port, address);
//    }
//
//    public LambdaMagnet(String name, Port port, int address) {
//        this(name, port, address, 300);
//    }


    @Override
    public void init() throws ControlException {
        super.init();
        controller.open();
    }

    @Override
    public void shutdown() throws ControlException {
        super.shutdown();
        try {
            controller.close();
            if (closePortOnShutDown) {
                controller.getPort().close();
            }
        } catch (Exception ex) {
            throw new ControlException("Failed to close the port", ex);
        }
    }

    /**
     * Method converts double to LAMBDA string
     *
     * @param d double that should be converted to string
     * @return string
     */
    private static String d2s(double d) {
        return LAMBDA_FORMAT.format(d);
    }

    public void setListener(MagnetStateListener listener) {
        this.listener = listener;
    }

//    public double getMeasuredI() {
//        return current;
//    }

//    @Override
//    public void acceptPhrase(String message) {
//
//    }
//
//    @Override
//    public void reportError(String errorMessage, Throwable error) {
//        if (this.listener != null) {
//            listener.error(getName(), errorMessage, error);
//        } else {
//            LoggerFactory.getLogger(getClass()).error(errorMessage, error);
//        }
//    }

    private void reportError(String errorMessage, Throwable error) {
        if (this.listener != null) {
            listener.error(getName(), errorMessage, error);
        } else {
            LoggerFactory.getLogger(getClass()).error(errorMessage, error);
        }
    }

    private String talk(String request) throws PortException {
        try {
            controller.send(request + "\r");
            return controller.waitFor(timeout).trim();
        } catch (PortTimeoutException tex) {
            //Single retry on timeout
            LoggerFactory.getLogger(getClass()).warn("A timeout exception for request '" + request + "'. Making another atempt.");
            controller.send(request + "\r");
            return controller.waitFor(timeout).trim();
        }
    }

    private String getParameter(String name) throws PortException {
        String res = talk(name + "?");
        return res;
    }

    private boolean setParameter(String name, String state) throws PortException {
        String res = talk(name + " " + state);
        return "OK".equals(res);
    }

    private boolean setParameter(String name, int state) throws PortException {
        String res = talk(name + " " + state);
        return "OK".equals(res);
    }

    private boolean setParameter(String name, double state) throws PortException {
        String res = talk(name + " " + d2s(state));
        return "OK".equals(res);
    }

    /**
     * Extract number from LAMBDA response
     *
     * @param str
     * @return
     */
    private double s2d(String str) {
        return Double.valueOf(str);
    }

    private double getCurrent() throws PortException {
        if (!setADR()) {
            if (listener != null) {
                listener.error(getName(), "Can't set address", null);
            }
            throw new PortException("Can't set address");
        }
        return s2d(getParameter("MC"));
    }

    protected void setCurrent(double current) throws PortException {
        if (!setParameter("PC", current)) {
            reportError("Can't set the current", null);
        } else {
            lastUpdate = DateTimeUtils.now();
        }
    }

    private boolean setADR() throws PortException {
        if (setParameter("ADR", getAddress())) {
            if (listener != null) {
                listener.addressChanged(getName(), address);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gets status of magnet for current moment
     *
     * @return status of magnet
     */
    private MagnetStatus getStatus() throws PortException {
        if (!setADR()) {
            return MagnetStatus.off();
        }

        boolean out;

        out = "ON".equals(talk("OUT?"));

        double measuredCurrent = s2d(getParameter("MC"));
        this.current = measuredCurrent;
        double setCurrent = s2d(getParameter("PC"));
        double measuredVoltage = s2d(getParameter("MV"));
        double setVoltage = s2d(getParameter("PV"));

        MagnetStatus monitor = new MagnetStatus(out, measuredCurrent, setCurrent, measuredVoltage, setVoltage);

        if (listener != null) {
            listener.acceptStatus(getName(), monitor);
        }
        return monitor;
    }

    /**
     * Cancel current update task
     */
    public void stopUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel(false);
            lastUpdate = null;
            if (listener != null) {
                listener.updateTaskStateChanged(getName(), false);
            }
        }
    }

    public void startUpdateTask(double targetI) {
        startUpdateTask(targetI, DEFAULT_DELAY);
    }

    /**
     * Start recursive updates of current with given delays between updates. If
     * delay is 0 then updates are made immediately.
     *
     * @param targetI
     * @param delay
     */
    public void startUpdateTask(double targetI, int delay) {
        assert delay > 0;
        stopUpdateTask();
        Runnable call = () -> {
            try {
                double measuredI = getCurrent();
                this.current = measuredI;

                if (listener != null) {
                    listener.acceptMeasuredI(getName(), measuredI);

                }

                if (Math.abs(measuredI - targetI) > CURRENT_PRECISION) {
                    double nextI = nextI(measuredI, targetI);

                    if (listener != null) {
                        listener.acceptNextI(getName(), nextI);
                    }
                    setCurrent(nextI);
                } else {
                    stopUpdateTask();
                }

            } catch (PortException ex) {
                reportError("Error in update task", ex);
                stopUpdateTask();
            }
        };

        updateTask = scheduler.scheduleWithFixedDelay(call, 0, delay, TimeUnit.MILLISECONDS);
        if (listener != null) {
            listener.updateTaskStateChanged(getName(), true);
        }
    }

    public void setOutputMode(boolean out) throws PortException {
        if (!setADR()) {
            throw new RuntimeException();
        }
        int outState;
        if (out) {
            outState = 1;
        } else {
            outState = 0;
        }
        if (!setParameter("OUT", outState)) {
            if (listener != null) {
                listener.error(getName(), "Can't set output mode", null);
            }
        } else if (listener != null) {
            listener.outputModeChanged(getName(), out);
        }
    }

    private double nextI(double measuredI, double targetI) {
        assert measuredI != targetI;

        double step;
        if (lastUpdate == null) {
            step = MIN_UP_STEP_SIZE;
        } else {
            //Choose optimal speed but do not exceed maximum speed
            step = Math.min(MAX_STEP_SIZE,
                    (double) lastUpdate.until(DateTimeUtils.now(), ChronoUnit.MILLIS) / 60000d * getSpeed());
        }

        double res;
        if (targetI > measuredI) {
            step = Math.max(MIN_UP_STEP_SIZE, step);
            res = Math.min(targetI, measuredI + step);
        } else {
            step = Math.max(MIN_DOWN_STEP_SIZE, step);
            res = Math.max(targetI, measuredI - step);
        }

        // не вводится ток меньше 0.5
        if (res < 0.5 && targetI > CURRENT_PRECISION) {
            return 0.5;
        } else if (res < 0.5 && targetI < CURRENT_PRECISION) {
            return 0;
        } else {
            return res;
        }
    }

    /**
     * Cancel current monitoring task
     */
    public void stopMonitorTask() {
        if (monitorTask != null) {
            monitorTask.cancel(true);
            if (listener != null) {
                listener.monitorTaskStateChanged(getName(), false);
            }
            monitorTask = null;
        }
    }

    public String getName() {
        if (this.name == null || this.name.isEmpty()) {
            return "LAMBDA " + getAddress();
        } else {
            return this.name;
        }
    }

    public void startMonitorTask() {
        startMonitorTask(DEFAULT_MONITOR_DELAY);
    }

    /**
     * Start monitoring task which checks for magnet status and then waits for
     * fixed time.
     *
     * @param delay an interval between scans in milliseconds
     */
    public void startMonitorTask(int delay) {
        assert delay >= 1000;
        stopMonitorTask();

        Runnable call = () -> {
            try {
                getStatus();
            } catch (PortException ex) {
                reportError("Port connection exception during status measurement", ex);
                stopMonitorTask();
            }
        };

        monitorTask = scheduler.scheduleWithFixedDelay(call, 0, delay, TimeUnit.MILLISECONDS);

        if (listener != null) {
            listener.monitorTaskStateChanged(getName(), true);
        }

    }

    public String request(String message) {
        try {
            if (!setADR()) {
                throw new RuntimeException("F")
            }
            return talk(message);
        } catch (PortException ex) {
            reportError("Can not send message to the port", ex);
            return null;
        }
    }

    /**
     * @return the address
     */
    public int getAddress() {
        return address;
    }

    /**
     * Get current change speed in Amper per minute
     *
     * @return
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * Set current change speed in Amper per minute
     *
     * @param speed
     */
    public void setSpeed(double speed) {
        this.speed = speed;
    }

}
