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
package inr.numass.control.msp;

import hep.dataforge.context.Context;
import hep.dataforge.control.connections.Roles;
import hep.dataforge.control.connections.StorageConnection;
import hep.dataforge.control.devices.SingleMeasurementDevice;
import hep.dataforge.control.devices.annotations.RoleDef;
import hep.dataforge.control.devices.annotations.StateDef;
import hep.dataforge.control.measurements.AbstractMeasurement;
import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.control.ports.PortHandler;
import hep.dataforge.control.ports.TcpPortHandler;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.exceptions.MeasurementException;
import hep.dataforge.exceptions.PortException;
import hep.dataforge.exceptions.StorageException;
import hep.dataforge.meta.Meta;
import hep.dataforge.storage.api.PointLoader;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.commons.LoaderFactory;
import hep.dataforge.tables.DataPoint;
import hep.dataforge.tables.MapPoint;
import hep.dataforge.tables.TableFormat;
import hep.dataforge.tables.TableFormatBuilder;
import hep.dataforge.utils.DateTimeUtils;
import hep.dataforge.values.Value;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;

/**
 * @author Alexander Nozik
 */
@RoleDef(name = Roles.STORAGE_ROLE, objectType = StorageConnection.class)
@RoleDef(name = Roles.VIEW_ROLE)
@StateDef(name = "connected", writable = true, info = "Connection with the device itself")
@StateDef(name = "storing", writable = true, info = "Define if this device is currently writes to storage")
@StateDef(name = "filamentOn", writable = true, info = "Mass-spectrometer filament on")
@StateDef(name = "filamentStatus", info = "Filament status")
public class MspDevice extends SingleMeasurementDevice implements PortHandler.PortController {
    public static final String MSP_DEVICE_TYPE = "msp";

    //    private static final String PEAK_SET_PATH = "peakJump.peak";
    private static final int TIMEOUT = 200;
//    private boolean connected = false;
//    private boolean selected = false;
//    private boolean controlled = false;
//    private boolean storing = false;

    private TcpPortHandler handler;
    //listener
    private MspListener mspListener;
    private Consumer<MspResponse> responseDelegate;

    public MspDevice() {
    }

    public MspDevice(Context context, Meta meta) {
        setContext(context);
        setMetaBase(meta);
    }

    //    public MspDevice(String name, Context context, Meta config) {
//        super(name, context, config);
//    }
    @Override
    public void init() throws ControlException {
        super.init();
        String ip = meta().getString("connection.ip", "127.0.0.1");
        int port = meta().getInt("connection.port", 10014);
        getLogger().info("Connection to MKS mass-spectrometer on {}:{}...", ip, port);
        handler = new TcpPortHandler(ip, port);
        handler.setDelimeter("\r\r");
    }

    @Override
    public void shutdown() throws ControlException {
        super.shutdown();
        super.stopMeasurement(true);
        if(isConnected()) {
            setFileamentOn(false);
            setConnected(false);
        }
        getHandler().close();
    }

    @Override
    protected Meta getMetaForMeasurement(String name) {
        switch (name) {
            case "peakJump":
                return meta().getMeta("peakJump");
            default:
                return super.getMetaForMeasurement(name);
        }
    }

    @Override
    protected Measurement createMeasurement(Meta meta) throws ControlException {
        switch (meta.getString("type", "peakJump")) {
            case "peakJump":
                return new PeakJumpMeasurement(meta);
            default:
                throw new ControlException("Unknown measurement type");
        }
    }

    @Override
    protected Object computeState(String stateName) throws ControlException {
        switch (stateName) {
            case "connected":
                return false;
            case "filamentOn":
                return false;//Always return false on first request
            case "filamentStatus":
                return "UNKNOWN";
            case "storing":
                return false;
            default:
                throw new ControlException("State not defined");
        }
    }

    @Override
    public String type() {
        return "MKS E-Vision";
    }

    @Override
    protected void requestStateChange(String stateName, Value value) throws ControlException {
        switch (stateName) {
            case "connected":
                setConnected(value.booleanValue());
            case "filamentOn":
                setFileamentOn(value.booleanValue());
            default:
                super.requestStateChange(stateName, value);
        }
    }

    /**
     * Startup MSP: get available sensors, select sensor and control.
     *
     * @param connected
     * @return
     * @throws hep.dataforge.exceptions.ControlException
     */
    private boolean setConnected(boolean connected) throws ControlException {
        String sensorName;
        if (isConnected() != connected) {
            if (connected) {
                handler.holdBy(this);
                MspResponse response = sendAndWait("Sensors");
                if (response.isOK()) {
                    sensorName = response.get(2, 1);
                } else {
                    error(response.errorDescription(), null);
                    return false;
                }
                //PENDING определеить в конфиге номер прибора

                response = sendAndWait("Select", sensorName);
                if (response.isOK()) {
                    updateState("selected", true);
//                    selected = true;
                } else {
                    error(response.errorDescription(), null);
                    return false;
                }

                response = sendAndWait("Control", "inr.numass.msp", "1.0");
                if (response.isOK()) {
//                    controlled = true;
//                    invalidateState("controlled");
                    updateState("controlled", true);
                } else {
                    error(response.errorDescription(), null);
                    return false;
                }
//                connected = true;
                updateState("connected", true);
                return true;
            } else {
                handler.unholdBy(this);
                return !sendAndWait("Release").isOK();
            }

        } else {
            return false;
        }
    }

    public void setMspListener(MspListener listener) {
        this.mspListener = listener;
    }

    /**
     * Send request to the msp
     *
     * @param command
     * @param parameters
     * @throws PortException
     */
    private void send(String command, Object... parameters) throws PortException {
        String request = buildCommand(command, parameters);
        if (mspListener != null) {
            mspListener.acceptRequest(request);
        }
        getHandler().send(request);
    }

    /**
     * A helper method to build msp command string
     *
     * @param command
     * @param parameters
     * @return
     */
    private String buildCommand(String command, Object... parameters) {
        StringBuilder builder = new StringBuilder(command);
        for (Object par : parameters) {
            builder.append(String.format(" \"%s\"", par.toString()));
        }
        builder.append("\n");
        return builder.toString();
    }

    /**
     * Send specific command and wait for its results (the result must begin
     * with command name)
     *
     * @param commandName
     * @param paremeters
     * @return
     * @throws PortException
     */
    private MspResponse sendAndWait(String commandName, Object... paremeters) throws PortException {

        String request = buildCommand(commandName, paremeters);
        if (mspListener != null) {
            mspListener.acceptRequest(request);
        }

        String response = getHandler().sendAndWait(
                request,
                (String str) -> str.trim().startsWith(commandName),
                TIMEOUT
        );
        return new MspResponse(response);
    }

    public boolean isConnected() {
        return getState("connected").booleanValue();
    }

    public boolean isSelected() {
        return getState("selected").booleanValue();
    }

    public boolean isControlled() {
        return getState("controlled").booleanValue();
    }

    public boolean isFilamentOn() {
        return getState("filamentOn").booleanValue();
    }

    public void selectFillament(int fillament) throws PortException {
        sendAndWait("FilamentSelect", fillament);
    }

    /**
     * Turn filament on or off
     *
     * @param filamentOn
     * @return
     * @throws hep.dataforge.exceptions.PortException
     */
    public boolean setFileamentOn(boolean filamentOn) throws PortException {
        if (filamentOn) {
            return sendAndWait("FilamentControl", "On").isOK();
        } else {
            return sendAndWait("FilamentControl", "Off").isOK();
        }
    }

    /**
     * Evaluate general async messages
     *
     * @param response
     */
    private void evaluateResponse(MspResponse response) {

    }

    @Override
    public void accept(String message) {
        if (mspListener != null) {
            mspListener.acceptMessage(message.trim());
        }
        MspResponse response = new MspResponse(message);

        switch (response.getCommandName()) {
            // all possible async messages
            case "FilamentStatus":
                String status = response.get(0, 2);
                updateState("filamentOn", status.equals("ON"));
                updateState("filamentStatus", status);
                if (mspListener != null) {
                    mspListener.acceptFilamentStateChange(status);
                }
                break;
        }
        if (responseDelegate != null) {
            responseDelegate.accept(response);
        }
    }

    @Override
    public void error(String errorMessage, Throwable error) {
        if (mspListener != null) {
            mspListener.error(errorMessage, error);
        } else if (error != null) {
            throw new RuntimeException(error);
        } else {
            throw new RuntimeException(errorMessage);
        }
    }

    private TcpPortHandler getHandler() {
        if (handler == null) {
            throw new RuntimeException("Device not initialized");
        }
        return handler;
    }

    /**
     * The MKS response as two-dimensional array of strings
     */
    public static class MspResponse {

        private final List<List<String>> data = new ArrayList<>();

        public MspResponse(String response) {
            String rx = "[^\"\\s]+|\"(\\\\.|[^\\\\\"])*\"";
            Scanner scanner = new Scanner(response.trim());

            while (scanner.hasNextLine()) {
                List<String> line = new ArrayList<>();
                String next = scanner.findWithinHorizon(rx, 0);
                while (next != null) {
                    line.add(next);
                    next = scanner.findInLine(rx);
                }
                data.add(line);
            }
        }

        public String getCommandName() {
            return this.get(0, 0);
        }

        public boolean isOK() {
            return "OK".equals(this.get(0, 1));
        }

        public int errorCode() {
            if (isOK()) {
                return -1;
            } else {
                return Integer.parseInt(get(1, 1));
            }
        }

        public String errorDescription() {
            if (isOK()) {
                return null;
            } else {
                return get(2, 1);
            }
        }

        public String get(int lineNo, int columnNo) {
            return data.get(lineNo).get(columnNo);
        }
    }

    private class PeakJumpMeasurement extends AbstractMeasurement<DataPoint> {

        private final Map<Integer, Double> measurement = new ConcurrentSkipListMap<>();
        private final Map<StorageConnection, PointLoader> loaderMap = new HashMap<>();
        //        private List<PointLoader> loaders = new ArrayList<>();
        private final Meta meta;
        private Map<Integer, String> peakMap;
        private double zero = 0;

        public PeakJumpMeasurement(Meta meta) {
            this.meta = meta;
        }

        private PointLoader makeLoader(StorageConnection connection) {

            try {
                Storage storage = connection.getStorage();

                if (peakMap == null) {
                    throw new IllegalStateException("Peak map is not initialized");
                }

                TableFormatBuilder builder = new TableFormatBuilder().addTime("timestamp");
                this.peakMap.values().forEach(builder::addNumber);

                TableFormat format = builder.build();

                String suffix = DateTimeUtils.now().toString();
                return LoaderFactory
                        .buildPointLoder(storage, "msp_" + suffix, "", "timestamp", format);
            } catch (StorageException ex) {
                getLogger().error("Failed to create Loader", ex);
                return null;
            }
        }

        @Override
        public void start() {
            responseDelegate = this::eval;

            try {
                String name = "peakJump";//an.getString("measurementNAmname", "default");
                String filterMode = meta.getString("filterMode", "PeakAverage");
                int accuracy = meta.getInt("accuracy", 5);
                //PENDING вставить остальные параметры?
                sendAndWait("MeasurementRemove", name);
                if (sendAndWait("AddPeakJump", name, filterMode, accuracy, 0, 0, 0).isOK()) {
                    peakMap = new LinkedHashMap<>();
                    for (Meta peak : meta.getMetaList("peak")) {
                        peakMap.put(peak.getInt("mass"), peak.getString("name", peak.getString("mass")));
                        if (!sendAndWait("MeasurementAddMass", peak.getString("mass")).isOK()) {
                            throw new ControlException("Can't add mass to measurement measurement for msp");
                        }
                    }
                } else {
                    throw new ControlException("Can't create measurement for msp");
                }

                if (!isFilamentOn()) {
                    this.error("Can't start measurement. Filament is not turned on.", null);
                }
                if (!sendAndWait("ScanAdd", "peakJump").isOK()) {
                    this.error("Failed to add scan", null);
                }

                if (!sendAndWait("ScanStart", 2).isOK()) {
                    this.error("Failed to start scan", null);
                }
            } catch (ControlException ex) {
                error(ex);
            }
            afterStart();
        }

        @Override
        public boolean stop(boolean force) throws MeasurementException {
            try {
                boolean stop = sendAndWait("ScanStop").isOK();
                afterStop();
                responseDelegate = null;
                loaderMap.values().forEach(loader -> {
                    try {
                        loader.close();
                    } catch (Exception ex) {
                        getLogger().error("Failed to close Loader", ex);
                    }
                });
                return stop;
            } catch (PortException ex) {
                throw new MeasurementException(ex);
            }
        }

        public void eval(MspResponse response) {

            //Evaluating device state change
            evaluateResponse(response);
            //Evaluating measurement information
            switch (response.getCommandName()) {
                case "MassReading":
                    double mass = Double.parseDouble(response.get(0, 1));
                    double value = Double.parseDouble(response.get(0, 2)) / 100d;
                    measurement.put((int) Math.floor(mass + 0.5), value);
                    break;
                case "ZeroReading":
                    zero = Double.parseDouble(response.get(0, 2)) / 100d;
                case "StartingScan":
                    if (mspListener != null && !measurement.isEmpty()) {
                        if (peakMap == null) {
                            throw new IllegalStateException("Peak map is not initialized");
                        }

                        if (isFilamentOn()) {

                            Instant time = DateTimeUtils.now();

                            MapPoint.Builder point = new MapPoint.Builder();
                            point.putValue("timestamp", time);

                            measurement.entrySet().forEach((entry) -> {
                                double val = entry.getValue();
                                point.putValue(peakMap.get(entry.getKey()), val);
                            });


                            mspListener.acceptScan(measurement);

                            if (getState("storing").booleanValue()) {
                                forEachConnection(Roles.STORAGE_ROLE, StorageConnection.class, (StorageConnection connection) -> {
                                    PointLoader pl = loaderMap.computeIfAbsent(connection, this::makeLoader);
                                    try {
                                        pl.push(point.build());
                                    } catch (StorageException ex) {
                                        getLogger().error("Push to loader failed", ex);
                                    }
                                });
                            }
                        }

                        measurement.clear();

                        int numScans = Integer.parseInt(response.get(0, 3));

                        if (numScans == 0) {
                            try {
                                send("ScanResume", 10);
                                //FIXME обработать ошибку связи
                            } catch (PortException ex) {
                                error(null, ex);
                            }
                        }

                        break;
                    }
            }
        }

        public void error(String errorMessage, Throwable error) {
            if (error == null) {
                error(new MeasurementException(errorMessage));
            } else {
                error(error);
            }
        }

    }
}
