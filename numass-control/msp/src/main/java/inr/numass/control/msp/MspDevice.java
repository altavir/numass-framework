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

import hep.dataforge.control.connections.Roles;
import hep.dataforge.control.connections.StorageConnection;
import hep.dataforge.control.devices.SingleMeasurementDevice;
import hep.dataforge.control.measurements.AbstractMeasurement;
import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.control.ports.PortHandler;
import hep.dataforge.control.ports.TcpPortHandler;
import hep.dataforge.data.DataFormat;
import hep.dataforge.data.DataFormatBuilder;
import hep.dataforge.data.DataPoint;
import hep.dataforge.data.MapDataPoint;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.exceptions.MeasurementException;
import hep.dataforge.exceptions.PortException;
import hep.dataforge.exceptions.StorageException;
import hep.dataforge.meta.Meta;
import hep.dataforge.storage.api.PointLoader;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.commons.LoaderFactory;
import hep.dataforge.values.Value;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;

/**
 *
 * @author Alexander Nozik
 */
public class MspDevice extends SingleMeasurementDevice implements PortHandler.PortController {

//    private static final String PEAK_SET_PATH = "peakJump.peak";
    private static final int TIMEOUT = 200;

    private TcpPortHandler handler;

    //listener
    private MspListener mspListener;

    private Consumer<MspResponse> responseDelegate;
    private Consumer<Throwable> errorDelegate;

//    public MspDevice(String name, Context context, Meta config) {
//        super(name, context, config);
//    }

    @Override
    public void init() throws ControlException {
        super.init();
        String ip = meta().getString("connection.ip", "127.0.0.1");
        int port = meta().getInt("connection.port", 10014);
        getLogger().info("Connection to MKS mass-spectrometer on {}:{}...", ip, port);
        handler = new TcpPortHandler(ip, port, "msp");
        handler.setDelimeter("\r\r");
        handler.holdBy(this);
        setConnected(true);
    }

    @Override
    public void shutdown() throws ControlException {
        super.shutdown();
        super.stopMeasurement(true);
        setFileamentOn(false);
        setConnected(false);
        handler.unholdBy(this);
        handler.close();
    }

    @Override
    protected Meta getMetaForMeasurement(String name) {
        switch (name) {
            case "peakJump":
                return meta().getNode("peakJump");
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
    protected Object calculateState(String stateName) throws ControlException {
        switch (stateName) {
            case "filamentOn":
                return false;//Always return false on first request
            case "filamentStatus":
                return "UNKNOWN";
            default:
                throw new ControlException("State not defined");
        }
    }

    @Override
    public String type() {
        return "MKS E-Vision";
    }

    @Override
    protected boolean applyState(String stateName, Value stateValue) throws ControlException {
        switch (stateName) {
            case "connected":
                return setConnected(stateValue.booleanValue());
            case "filamentOn":
                return setFileamentOn(stateValue.booleanValue());
            default:
                return super.applyState(stateName, stateValue);
        }
    }

    /**
     * Startup MSP: get available sensors, select sensor and control.
     *
     * @param measurement
     * @throws hep.dataforge.exceptions.PortException
     */
    public boolean setConnected(boolean connected) throws ControlException {
        String sensorName;
        if (isConnected() != connected) {
            if (connected) {
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
                } else {
                    error(response.errorDescription(), null);
                    return false;
                }

                response = sendAndWait("Control", "inr.numass.msp", "1.0");
                if (response.isOK()) {
                    updateState("controlled", true);
                } else {
                    error(response.errorDescription(), null);
                    return false;
                }
                updateState("connected", true);
                return true;
            } else {
                return !sendAndWait("Release").isOK();
            }

        } else {
            return false;
        }
    }

    public void setListener(MspListener listener) {
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
        handler.send(request);
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
     * Send specific command and wait for its results (the onResult must begin
     * with command name)
     *
     * @param commandName
     * @param paremeters
     * @return
     * @throws PortException
     */
    public MspResponse sendAndWait(String commandName, Object... paremeters) throws PortException {

        String request = buildCommand(commandName, paremeters);
        if (mspListener != null) {
            mspListener.acceptRequest(request);
        }

        String response = handler.sendAndWait(
                request,
                (String str) -> str.trim().startsWith(commandName),
                TIMEOUT
        );
        return new MspResponse(response);
    }

    public boolean isConnected() {
        return getState("connected") != null && getState("connected").booleanValue();
    }

    public boolean isSelected() {
        return getState("selected") != null && getState("selected").booleanValue();
    }

    public boolean isControlled() {
        return getState("controlled") != null && getState("controlled").booleanValue();
    }

    public boolean isFilamentOn() {
        return getState("filamentOn").booleanValue();
    }

    /**
     * Turn filament on or off
     *
     * @return
     *
     * @param filamentOn
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
                    mspListener.acceptFillamentStateChange(status);
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
        private Map<Integer, String> peakMap;
        private List<PointLoader> loaders = new ArrayList<>();
        private final Meta meta;

        public PeakJumpMeasurement(Meta meta) {
            this.meta = meta;
        }

        private void prepareLoaders() {
            loaders = new ArrayList<>();
            forEachTypedConnection(Roles.STORAGE_ROLE, StorageConnection.class, (StorageConnection con) -> {
                try {
                    Storage storage = con.getStorage();

                    if (peakMap == null) {
                        throw new IllegalStateException("Peak map is not initialized");
                    }

                    DataFormatBuilder builder = new DataFormatBuilder().addTime("timestamp");
                    for (String peakName : this.peakMap.values()) {
                        builder.addNumber(peakName);
                    }

                    DataFormat format = builder.build();

                    //TODO Переделать!!!
                    String run = meta().getString("numass.run", "");

                    String suffix = Integer.toString((int) Instant.now().toEpochMilli());
                    PointLoader loader = LoaderFactory
                            .buildPointLoder(storage, "msp" + suffix, run, "timestamp", format);
                    loaders.add(loader);
                } catch (StorageException ex) {
                    getLogger().error("Failed to initialize peak jump loader", ex);
                }
            });
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
                    for (Meta peak : meta.getNodes("peak")) {
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
                onError(ex);
            }
            onStart();
        }

        @Override
        public boolean stop(boolean force) throws MeasurementException {
            try {
                boolean stop = sendAndWait("ScanStop").isOK();
                onFinish();
                responseDelegate = null;
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
                    double value = Double.parseDouble(response.get(0, 2));
                    measurement.put((int) Math.floor(mass + 0.5), value);
                    break;
                case "StartingScan":
                    if (mspListener != null && !measurement.isEmpty()) {
                        if (peakMap == null) {
                            throw new IllegalStateException("Peal map is not initialized");
                        }

                        Instant time = Instant.now();

                        MapDataPoint point = new MapDataPoint();
                        point.putValue("timestamp", time);

                        measurement.entrySet().stream().forEach((entry) -> {
                            double val = entry.getValue();
                            point.putValue(peakMap.get(entry.getKey()), val);
                        });

                        if (isFilamentOn()) {
                            mspListener.acceptScan(measurement);

                            for (PointLoader loader : this.loaders) {
                                try {
                                    loader.push(point);
                                } catch (StorageException ex) {
                                    getLogger().error("Push to repo failed", ex);
                                }
                            }
                        }

                        measurement.clear();

                        int numScans = Integer.parseInt(response.get(0, 3));

                        if (numScans == 0) {
                            try {
                                send("ScanResume", 2);
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
                onError(new MeasurementException(errorMessage));
            } else {
                onError(error);
            }
        }

    }
}
