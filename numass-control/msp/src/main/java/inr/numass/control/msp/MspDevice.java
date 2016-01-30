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
import hep.dataforge.context.GlobalContext;
import hep.dataforge.control.measurements.DataDevice;
import hep.dataforge.control.ports.PortHandler;
import hep.dataforge.control.ports.TcpPortHandler;
import hep.dataforge.data.DataFormat;
import hep.dataforge.data.DataFormatBuilder;
import hep.dataforge.data.MapDataPoint;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.exceptions.PortException;
import hep.dataforge.exceptions.StorageException;
import hep.dataforge.meta.Meta;
import hep.dataforge.storage.api.PointLoader;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.commons.LoaderFactory;
import hep.dataforge.storage.commons.StoragePlugin;
import hep.dataforge.storage.loaders.ChainPointLoader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentSkipListMap;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Alexander Nozik
 */
public class MspDevice extends DataDevice implements PortHandler.PortController {

//    private static final String PEAK_SET_PATH = "peakJump.peak";
    private static final int TIMEOUT = 200;

    //storage
//    /**
//     * Separate storage configuration for backup storage.
//     */
//    private Meta backupStorage;
    private PointLoader peakJumpLoader;

    //port handler
    private TcpPortHandler handler;

    //listener
    private MspListener mspListener;

    private String sensorName = null;
    private boolean isSelected = false;
    private boolean isControlled = false;
    private boolean isFilamentOn = false;
//    private boolean isScanning = false;

    private final Map<Integer, Double> measurement = new ConcurrentSkipListMap<>();
    private Map<Integer, String> peakMap;

    public MspDevice(String name, Meta annotation) {
        super(name, GlobalContext.instance(), annotation);
    }

    public MspDevice(String name, Context context, Meta config) {
        super(name, context, config);
    }

    @Override
    public void init() throws ControlException {
        super.init();
        String ip = meta().getString("connection.ip", "127.0.0.1");
        int port = meta().getInt("connection.port", 10014);
        getLogger().info("Connection to MKS mass-spectrometer on {}:{}...", ip, port);
        handler = new TcpPortHandler(ip, port, "msp");
        handler.setDelimeter("\r\r");
    }

    /**
     * Startup MSP: get available sensors, select sensor and control.
     *
     * @param measurement
     * @throws hep.dataforge.exceptions.PortException
     */
    @Override
    public void doStart(Meta measurement) throws ControlException {
        if (!isControlled) {
            if (handler.isLocked()) {
                LoggerFactory.getLogger(getClass()).error("Trying to init MSP controller on locked port. Breaking the lock.");
                handler.breakHold();
            }
            handler.holdBy(this);
            MspResponse response = sendAndWait("Sensors");
            if (response.isOK()) {
                this.sensorName = response.get(2, 1);
            } else {
                error(response.errorDescription(), null);
                return;
            }
            //PENDING определеить в конфиге номер прибора

            response = sendAndWait("Select", sensorName);
            if (response.isOK()) {
                this.isSelected = true;
            } else {
                error(response.errorDescription(), null);
                return;
            }

            response = sendAndWait("Control", "inr.numass.msp", "1.0");
            if (response.isOK()) {
                this.isControlled = true;
            } else {
                error(response.errorDescription(), null);
            }
        }
        createPeakJumpMeasurement(buildMeasurementLaminate(measurement).getNode("peakJump"));
        this.peakJumpLoader = getPeakJumpLoader(measurement);
    }

//    public void setStorageConfig(Meta storageConfig, List<String> peaks) throws StorageException {
//        Storage storage = getContext().provide("storage", StoragePlugin.class).buildStorage(storageConfig);
//        String suffix = Integer.toString((int) Instant.now().toEpochMilli());
//
//        this.peakJumpLoader = LoaderFactory.buildPointLoder(storage, "msp" + suffix, "", "timestamp", DataFormat.forNames(10, peaks));
//    }

    private PointLoader getPeakJumpLoader(Meta measurement) {
        if (peakJumpLoader == null) {
            try {
//                StoragePlugin plugin = getContext().provide("storage", StoragePlugin.class);

                Storage primaryStorage = getPrimaryStorage(measurement);

                if (peakMap == null) {
                    throw new IllegalStateException("Peak map is not initialized");
                }

                DataFormatBuilder builder = new DataFormatBuilder().addTime("timestamp");
                for (String peakName : this.peakMap.values()) {
                    builder.addNumber(peakName);
                }

                DataFormat format = builder.build();

                //TODO Переделать!!!
                String run = meta().getString("numass.run","");
                
                String suffix = Integer.toString((int) Instant.now().toEpochMilli());
                peakJumpLoader = LoaderFactory
                        .buildPointLoder(primaryStorage, "msp" + suffix, run, "timestamp", format);
                


                try {
                    Storage secondaryStorage = getSecondaryStorage(measurement);
                    if (secondaryStorage != null) {
                        PointLoader backupLoader = LoaderFactory
                                .buildPointLoder(secondaryStorage, "msp" + suffix, run, "timestamp", format);
                        peakJumpLoader = new ChainPointLoader(peakJumpLoader, backupLoader);
                    }
                } catch (Exception ex) {
                    getLogger().error("Failed to initialize backup peak jump loader", ex);
                }

            } catch (StorageException ex) {
                getLogger().error("Failed to initialize primary peak jump loader", ex);
                return null;
            }

        }
        return peakJumpLoader;
    }

    public void setListener(MspListener listener) {
        this.mspListener = listener;
    }

    private void send(String command, Object... parameters) throws PortException {
        String request = buildCommand(command, parameters);
        if (mspListener != null) {
            mspListener.acceptRequest(request);
        }
        handler.send(request);
    }

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

    @Override
    public synchronized void accept(String message) {
        if (mspListener != null) {
            mspListener.acceptMessage(message.trim());
        }

        MspResponse response = new MspResponse(message);
        switch (response.getCommandName()) {
            // all possible async messages
            case "FilamentStatus":
                String status = response.get(0, 2);
                isFilamentOn = status.equals("ON");
                if (this.mspListener != null) {
                    this.mspListener.acceptFillamentStateChange(status);
                }
                break;
            case "MassReading":
                double mass = Double.parseDouble(response.get(0, 1));
                double value = Double.parseDouble(response.get(0, 2));
                this.measurement.put((int) Math.floor(mass + 0.5), value);
                break;
            case "StartingScan":
                if (this.mspListener != null && !measurement.isEmpty() && isFilamentOn) {

                    if (peakMap == null) {
                        throw new IllegalStateException("Peal map is not initialized");
                    }

                    mspListener.acceptMeasurement(measurement);

                    Instant time = Instant.now();

                    MapDataPoint point = new MapDataPoint();
                    point.putValue("timestamp", time);

                    for (Map.Entry<Integer, Double> entry : measurement.entrySet()) {
                        double val = entry.getValue();
                        point.putValue(peakMap.get(entry.getKey()), val);
                    }

                    if (peakJumpLoader != null) {
                        try {
                            peakJumpLoader.push(point);
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

    public boolean isSelected() {
        return isSelected;
    }

    public boolean isControlled() {
        return isControlled;
    }

    public boolean isFilamentOn() {
        return isFilamentOn;
    }

//    public boolean isIsScanning() {
//        return isScanning;
//    }
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
     * Create measurement with parameters and return its name
     *
     * @param an
     * @return
     * @throws hep.dataforge.exceptions.PortException
     */
    private void createPeakJumpMeasurement(Meta an) throws ControlException {
        String name = "peakJump";//an.getString("measurementNAmname", "default");
        String filterMode = an.getString("filterMode", "PeakAverage");
        int accuracy = an.getInt("accuracy", 5);
        //PENDING вставить остальные параметры?
        sendAndWait("MeasurementRemove", name);
        if (sendAndWait("AddPeakJump", name, filterMode, accuracy, 0, 0, 0).isOK()) {
            peakMap = new LinkedHashMap<>();
            for (Meta peak : an.getNodes("peak")) {
                peakMap.put(peak.getInt("mass"), peak.getString("name", peak.getString("mass")));
                if (!sendAndWait("MeasurementAddMass", peak.getString("mass")).isOK()) {
                    throw new ControlException("Can't add mass to measurement measurement for msp");
                }
            }
        } else {
            throw new ControlException("Can't create measurement for msp");
        }
    }

    public boolean startPeakJumpMeasurement() throws PortException {
        if (!isFilamentOn()) {
            error("Can't start measurement. Filament is not turned on.", null);
        }
      
        if (!sendAndWait("ScanAdd", "peakJump").isOK()) {
            return false;
        }
        return sendAndWait("ScanStart", 2).isOK();
    }


    public boolean stopMeasurement() throws PortException {
        return sendAndWait("ScanStop").isOK();
    }

    @Override
    public void doStop() throws PortException {
        stopMeasurement();
        setFileamentOn(false);
        sendAndWait("Release");
        handler.unholdBy(this);
        handler.close();
    }

    @Override
    public void error(String errorMessage, Throwable error) {
        if (mspListener != null) {
            mspListener.error(errorMessage, error);
        } else {
            if (error != null) {
                throw new RuntimeException(error);
            } else {
                throw new RuntimeException(errorMessage);
            }
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

}
