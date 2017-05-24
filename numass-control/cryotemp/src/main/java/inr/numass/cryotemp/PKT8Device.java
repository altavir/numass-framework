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
package inr.numass.cryotemp;

import hep.dataforge.context.Context;
import hep.dataforge.control.RoleDef;
import hep.dataforge.control.collectors.RegularPointCollector;
import hep.dataforge.control.connections.Roles;
import hep.dataforge.control.connections.StorageConnection;
import hep.dataforge.control.devices.Device;
import hep.dataforge.control.devices.PortSensor;
import hep.dataforge.control.measurements.AbstractMeasurement;
import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.control.ports.PortHandler;
import hep.dataforge.description.ValueDef;
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
import inr.numass.control.StorageHelper;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A device controller for Dubna PKT 8 cryogenic thermometry device
 *
 * @author Alexander Nozik
 */
@RoleDef(name = Roles.STORAGE_ROLE)
@RoleDef(name = Roles.VIEW_ROLE)
@ValueDef(name = "port", def = "virtual", info = "The name of the port for this PKT8")
public class PKT8Device extends PortSensor<PKT8Result> {
    public static final String PKT8_DEVICE_TYPE = "numass:pkt8";

    public static final String PGA = "pga";
    public static final String SPS = "sps";
    public static final String ABUF = "abuf";
    private static final String[] CHANNEL_DESIGNATIONS = {"a", "b", "c", "d", "e", "f", "g", "h"};
    /**
     * The key is the letter (a,b,c,d...) as in measurements
     */
    private final Map<String, PKT8Channel> channels = new LinkedHashMap<>();
    private RegularPointCollector collector;
    private StorageHelper storageHelper;

    /**
     * Cached values
     */
    private TableFormat format;

    public PKT8Device() {
    }


    public PKT8Device(Context context, Meta meta) {
        setContext(context);
        setMeta(meta);
    }

    private PointLoader buildLoader(StorageConnection connection) {
        Storage storage = connection.getStorage();
        String suffix = DateTimeUtils.fileSuffix();

        try {
            return LoaderFactory.buildPointLoder(storage,
                    "cryotemp_" + suffix, "", "timestamp", getTableFormat());
        } catch (StorageException e) {
            throw new RuntimeException("Failed to build loader from storage", e);
        }
    }

    @Override
    public void init() throws ControlException {

        //read channel configuration
        if (meta().hasMeta("channel")) {
            for (Meta node : meta().getMetaList("channel")) {
                String designation = node.getString("designation", "default");
                this.channels.put(designation, new PKT8Channel(node));
            }
        } else {
            //set default channel configuration
            for (String designation : CHANNEL_DESIGNATIONS) {
                channels.put(designation, new PKT8Channel(designation));
            }
            getLogger().warn("No channels defined in configuration");
        }

        super.init();

        //update parameters from meta
        if (meta().hasValue("pga")) {
            getLogger().info("Setting dynamic range to " + meta().getInt("pga"));
            String response = getHandler().sendAndWait("g" + meta().getInt("pga"), 400).trim();
            if (response.contains("=")) {
                updateState(PGA, Integer.parseInt(response.substring(4)));
            } else {
                getLogger().error("Setting pga failsed with message: " + response);
            }
        }

        setSPS(meta().getInt("sps", 0));
        setBUF(meta().getInt("abuf", 100));

        // setting up the collector
        storageHelper = new StorageHelper(this, this::buildLoader);
        Duration duration = Duration.parse(meta().getString("averagingDuration", "PT30S"));
        collector = new RegularPointCollector(
                duration,
                channels.values().stream().map(PKT8Channel::getName).collect(Collectors.toList()),
                (DataPoint dp) -> {
                    getLogger().debug("Point measurement complete. Pushing...");
                    storageHelper.push(dp);
                });
    }


    private TableFormat getTableFormat() {
        if (format == null) {
            // Building data format
            TableFormatBuilder tableFormatBuilder = new TableFormatBuilder()
                    .addTime("timestamp");

            for (PKT8Channel channel : channels.values()) {
                tableFormatBuilder.addNumber(channel.getName());
            }
            format = tableFormatBuilder.build();
        }
        return format;
    }

    @Override
    public void shutdown() throws ControlException {
        storageHelper.close();
        if (collector != null) {
            collector.clear();
            collector = null;
        }
        super.shutdown();
    }

    @Override
    protected PortHandler buildHandler(String portName) throws ControlException {
        PortHandler handler;
        //setup connection
        if ("virtual".equals(portName)) {
            getLogger().info("Starting {} using virtual debug port", getName());
            handler = new PKT8VirtualPort("PKT8", meta().getMetaOrEmpty("debug"));
        } else {
            handler = super.buildHandler(portName);
        }
        handler.setDelimeter("\n");

        return handler;
    }

    public Collection<PKT8Channel> getChanels() {
        return this.channels.values();
    }

    private void setBUF(int buf) throws ControlException {
        getLogger().info("Setting avaraging buffer size to " + buf);
        String response;
        try {
            response = getHandler().sendAndWait("b" + buf, 400).trim();
        } catch (Exception ex) {
            response = ex.getMessage();
        }

        if (response.contains("=")) {
            updateState(ABUF, Integer.parseInt(response.substring(14)));
//            getLogger().info("successfully set buffer size to {}", this.abuf);
        } else {
            getLogger().error("Setting averaging buffer failed with message: " + response);
        }
    }

    public void changeParameters(int sps, int abuf) throws ControlException {
        stopMeasurement(false);
        //setting sps
        setSPS(sps);
        //setting buffer
        setBUF(abuf);
    }

    /**
     * '0' : 2,5 SPS '1' : 5 SPS '2' : 10 SPS '3' : 25 SPS '4' : 50 SPS '5' :
     * 100 SPS '6' : 500 SPS '7' : 1 kSPS '8' : 3,75 kSPS
     *
     * @param sps
     * @return
     */
    private String spsToStr(int sps) {
        switch (sps) {
            case 0:
                return "2.5 SPS";
            case 1:
                return "5 SPS";
            case 2:
                return "10 SPS";
            case 3:
                return "25 SPS";
            case 4:
                return "50 SPS";
            case 5:
                return "100 SPS";
            case 6:
                return "500 SPS";
            case 7:
                return "1 kSPS";
            case 8:
                return "3.75 kSPS";
            default:
                return "unknown value";
        }
    }

    /**
     * '0' : ± 5 В '1' : ± 2,5 В '2' : ± 1,25 В '3' : ± 0,625 В '4' : ± 312.5 мВ
     * '5' : ± 156,25 мВ '6' : ± 78,125 мВ
     *
     * @param pga
     * @return
     */
    private String pgaToStr(int pga) {
        switch (pga) {
            case 0:
                return "± 5 V";
            case 1:
                return "± 2,5 V";
            case 2:
                return "± 1,25 V";
            case 3:
                return "± 0,625 V";
            case 4:
                return "± 312.5 mV";
            case 5:
                return "± 156.25 mV";
            case 6:
                return "± 78.125 mV";
            default:
                return "unknown value";
        }
    }

    public String getSPS() {
        return getState(SPS).stringValue();
    }

    private void setSPS(int sps) throws ControlException {
        getLogger().info("Setting sampling rate to " + spsToStr(sps));
        String response;
        try {
            response = getHandler().sendAndWait("v" + sps, 400).trim();
        } catch (Exception ex) {
            response = ex.getMessage();
        }
        if (response.contains("=")) {
            updateState(SPS, Integer.parseInt(response.substring(4)));
//            getLogger().info("successfully sampling rate to {}", spsToStr(this.sps));
        } else {
            getLogger().error("Setting sps failsed with message: " + response);
        }
    }

    public String getPGA() {
        return getState(PGA).stringValue();
    }

    public String getABUF() {
        return getState(ABUF).stringValue();
    }

//    public void connectPointListener(PointListenerConnection listener) {
//        this.connect(listener, Roles.POINT_LISTENER_ROLE);
//    }

    @Override
    protected Measurement<PKT8Result> createMeasurement() throws MeasurementException {
        if (this.getMeasurement() != null) {
            return this.getMeasurement();
        } else {
            try {
                if (getHandler().isLocked()) {
                    getLogger().error("Breaking hold on handler because it is locked");
                    getHandler().breakHold();
                }
                return new PKT8Measurement(getHandler());
            } catch (ControlException e) {
                throw new MeasurementException(e);
            }
        }
    }

    @Override
    public Measurement<PKT8Result> startMeasurement() throws MeasurementException {
        //clearing PKT queue
        try {
            getHandler().send("p");
            getHandler().sendAndWait("p", 400);
        } catch (ControlException e) {
            getLogger().error("Failed to clear PKT8 port");
            //   throw new MeasurementException(e);
        }
        return super.startMeasurement();
    }


    public class PKT8Measurement extends AbstractMeasurement<PKT8Result> implements PortHandler.PortController {

        final PortHandler handler;


        public PKT8Measurement(PortHandler handler) {
            this.handler = handler;
        }

        @Override
        public Device getDevice() {
            return PKT8Device.this;
        }

        @Override
        public void start() {
            if (isStarted()) {
                getLogger().warn("Trying to start measurement which is already started");
            }

            try {
                getLogger().info("Starting measurement");
                handler.holdBy(this);
                handler.send("s");
                afterStart();
            } catch (PortException ex) {
                error("Failed to start measurement", ex);
            }

        }

        @Override
        public boolean stop(boolean force) throws MeasurementException {
            if (isFinished()) {
                getLogger().warn("Trying to stop measurement which is already stopped");
            }

            try {
                getLogger().info("Stopping measurement");
                String response = getHandler().sendAndWait("p", 400).trim();
                // Должно быть именно с большой буквы!!!
                return "Stopped".equals(response) || "stopped".equals(response);
            } catch (Exception ex) {
                error(ex);
                return false;
            } finally {
                if (collector != null) {
                    collector.clear();
                }
                handler.unholdBy(this);
            }
        }


        @Override
        public void accept(String message) {
            String trimmed = message.trim();

            if (isStarted()) {
                if (trimmed.equals("Stopped") || trimmed.equals("stopped")) {
                    afterPause();
//                    getLogger().info("Measurement stopped");
                } else {
                    String designation = trimmed.substring(0, 1);
                    double rawValue = Double.parseDouble(trimmed.substring(1)) / 100;

                    if (channels.containsKey(designation)) {
                        PKT8Channel channel = channels.get(designation);
                        result(channel.evaluate(rawValue));
                        if (collector != null) {
                            collector.put(channel.getName(), channel.getTemperature(rawValue));
                        }
                    } else {
                        result(new PKT8Result(designation, rawValue, -1));
                    }
                }
            }
        }

        @Override
        public void error(String errorMessage, Throwable error) {
            super.error(error);
        }
    }
}