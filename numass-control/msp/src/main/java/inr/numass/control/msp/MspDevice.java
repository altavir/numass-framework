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
import hep.dataforge.control.NamedValueListener;
import hep.dataforge.control.RoleDef;
import hep.dataforge.control.collectors.RegularPointCollector;
import hep.dataforge.control.connections.Roles;
import hep.dataforge.control.connections.StorageConnection;
import hep.dataforge.control.devices.Device;
import hep.dataforge.control.devices.PortSensor;
import hep.dataforge.control.devices.Sensor;
import hep.dataforge.control.devices.StateDef;
import hep.dataforge.control.measurements.AbstractMeasurement;
import hep.dataforge.control.ports.PortHandler;
import hep.dataforge.control.ports.TcpPortHandler;
import hep.dataforge.description.ValueDef;
import hep.dataforge.events.EventBuilder;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.exceptions.MeasurementException;
import hep.dataforge.exceptions.PortException;
import hep.dataforge.exceptions.StorageException;
import hep.dataforge.meta.Meta;
import hep.dataforge.storage.api.PointLoader;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.commons.LoaderFactory;
import hep.dataforge.tables.DataPoint;
import hep.dataforge.tables.TableFormat;
import hep.dataforge.tables.TableFormatBuilder;
import hep.dataforge.utils.DateTimeUtils;
import hep.dataforge.values.Value;
import inr.numass.control.StorageHelper;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author Alexander Nozik
 */
@RoleDef(name = Roles.STORAGE_ROLE, objectType = StorageConnection.class)
@RoleDef(name = Roles.VIEW_ROLE)
@StateDef(
        value = @ValueDef(name = PortSensor.CONNECTED_STATE, info = "Connection with the device itself"),
        writable = true
)
@StateDef(
        value = @ValueDef(name = "storing", info = "Define if this device is currently writes to storage"),
        writable = true
)
@StateDef(value = @ValueDef(name = "filamentOn", info = "Mass-spectrometer filament on"), writable = true)
@StateDef(@ValueDef(name = "filamentStatus", info = "Filament status"))
public class MspDevice extends Sensor<DataPoint> implements PortHandler.PortController {
    public static final String MSP_DEVICE_TYPE = "msp";

    private static final int TIMEOUT = 200;

    private TcpPortHandler handler;
    private Consumer<MspResponse> measurementDelegate;

    public MspDevice() {
    }

    public MspDevice(Context context, Meta meta) {
        setContext(context);
        setMeta(meta);
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
        super.stopMeasurement(true);
        if (isConnected()) {
            setFilamentOn(false);
            setConnected(false);
        }
        getHandler().close();
        super.shutdown();
    }

//    @Override
//    protected Meta getMeasurementMeta() {
//        return meta().getMeta("peakJump");
//    }

    @Override
    protected PeakJumpMeasurement createMeasurement() throws MeasurementException {
        Meta measurementMeta = meta().getMeta("peakJump");
        String s = measurementMeta.getString("type", "peakJump");
        if (s.equals("peakJump")) {
            PeakJumpMeasurement measurement = new PeakJumpMeasurement(measurementMeta);
            this.measurementDelegate = measurement;
            return measurement;
        } else {
            throw new MeasurementException("Unknown measurement type");
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
                return super.computeState(stateName);
        }
    }

    @Override
    public String type() {
        return "MKS E-Vision";
    }

    @Override
    protected void requestStateChange(String stateName, Value value) throws ControlException {
        switch (stateName) {
            case PortSensor.CONNECTED_STATE:
                setConnected(value.booleanValue());
            case "filamentOn":
                setFilamentOn(value.booleanValue());
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
                updateState(PortSensor.CONNECTED_STATE, true);
                return true;
            } else {
                handler.unholdBy(this);
                return !sendAndWait("Release").isOK();
            }

        } else {
            return false;
        }
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
        dispatchEvent(
                EventBuilder
                        .make("msp")
                        .setMetaValue("request", request)
                        .build()
        );
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
     * @param parameters
     * @return
     * @throws PortException
     */
    private MspResponse sendAndWait(String commandName, Object... parameters) throws PortException {

        String request = buildCommand(commandName, parameters);
        dispatchEvent(
                EventBuilder
                        .make("msp")
                        .setMetaValue("request", request)
                        .build()
        );

        String response = getHandler().sendAndWait(
                request,
                TIMEOUT,
                (String str) -> str.trim().startsWith(commandName)
        );
        return new MspResponse(response);
    }

    public boolean isConnected() {
        return getState(PortSensor.CONNECTED_STATE).booleanValue();
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

    public void selectFillament(int filament) throws PortException {
        sendAndWait("FilamentSelect", filament);
    }

    /**
     * Turn filament on or off
     *
     * @param filamentOn
     * @return
     * @throws hep.dataforge.exceptions.PortException
     */
    public boolean setFilamentOn(boolean filamentOn) throws PortException {
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
        dispatchEvent(
                EventBuilder
                        .make("msp")
                        .setMetaValue("response", message.trim()).build()
        );
        MspResponse response = new MspResponse(message);

        switch (response.getCommandName()) {
            // all possible async messages
            case "FilamentStatus":
                String status = response.get(0, 2);
                updateState("filamentOn", status.equals("ON"));
                updateState("filamentStatus", status);
                break;
        }
        if (measurementDelegate != null) {
            measurementDelegate.accept(response);
        }
    }

    @Override
    public void error(String errorMessage, Throwable error) {
        notifyError(errorMessage, error);
    }

    private TcpPortHandler getHandler() {
        if (handler == null) {
            throw new RuntimeException("Device not initialized");
        }
        return handler;
    }

    private Duration getAveragingDuration() {
        return Duration.parse(meta().getString("averagingDuration", "PT30S"));
    }

    /**
     * The MKS response as two-dimensional array of strings
     */
    static class MspResponse {

        private final List<List<String>> data = new ArrayList<>();

        MspResponse(String response) {
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

        String getCommandName() {
            return this.get(0, 0);
        }

        boolean isOK() {
            return "OK".equals(this.get(0, 1));
        }

        int errorCode() {
            if (isOK()) {
                return -1;
            } else {
                return Integer.parseInt(get(1, 1));
            }
        }

        String errorDescription() {
            if (isOK()) {
                return null;
            } else {
                return get(2, 1);
            }
        }

        String get(int lineNo, int columnNo) {
            return data.get(lineNo).get(columnNo);
        }
    }

    public class PeakJumpMeasurement extends AbstractMeasurement<DataPoint> implements Consumer<MspResponse> {

        private RegularPointCollector collector = new RegularPointCollector(getAveragingDuration(), this::result);
        private StorageHelper helper = new StorageHelper(MspDevice.this, this::makeLoader);
        private final Meta meta;
        private Map<Integer, String> peakMap;
        private double zero = 0;

        private PeakJumpMeasurement(Meta meta) {
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

                String suffix = DateTimeUtils.fileSuffix();
                return LoaderFactory
                        .buildPointLoder(storage, "msp_" + suffix, "", "timestamp", format);
            } catch (StorageException ex) {
                getLogger().error("Failed to create Loader", ex);
                return null;
            }
        }

        @Override
        public Device getDevice() {
            return MspDevice.this;
        }

        @Override
        public void start() {
            try {
                String measurementName = "peakJump";
                String filterMode = meta.getString("filterMode", "PeakAverage");
                int accuracy = meta.getInt("accuracy", 5);
                //PENDING вставить остальные параметры?
                sendAndWait("MeasurementRemoveAll");
                if (sendAndWait("AddPeakJump", measurementName, filterMode, accuracy, 0, 0, 0).isOK()) {
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
                if (!sendAndWait("ScanAdd", measurementName).isOK()) {
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
                collector.stop();
                boolean stop = sendAndWait("ScanStop").isOK();
                afterStop();
                helper.close();
                return stop;
            } catch (PortException ex) {
                throw new MeasurementException(ex);
            }
        }

        @Override
        protected synchronized void result(DataPoint result, Instant time) {
            super.result(result, time);
            helper.push(result);
        }

        void error(String errorMessage, Throwable error) {
            if (error == null) {
                error(new MeasurementException(errorMessage));
            } else {
                error(error);
            }
        }

        @Override
        public void accept(MspResponse response) {

            //Evaluating device state change
            evaluateResponse(response);
            //Evaluating measurement information
            switch (response.getCommandName()) {
                case "MassReading":
                    double mass = Double.parseDouble(response.get(0, 1));
                    double value = Double.parseDouble(response.get(0, 2)) / 100d;
                    String massName = Integer.toString((int) Math.floor(mass + 0.5));
                    collector.put(massName, value);
                    forEachConnection(Roles.VIEW_ROLE, NamedValueListener.class, listener -> listener.pushValue(massName, value));
                    break;
                case "ZeroReading":
                    zero = Double.parseDouble(response.get(0, 2)) / 100d;
                    break;
                case "StartingScan":
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
}
