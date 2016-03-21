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

import hep.dataforge.names.Named;
import hep.dataforge.context.Context;
import hep.dataforge.control.collectors.RegularPointCollector;
import hep.dataforge.control.measurements.DataDevice;
import hep.dataforge.control.ports.PortHandler;
import hep.dataforge.control.ports.TcpPortHandler;
import hep.dataforge.points.FormatBuilder;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.exceptions.PortException;
import hep.dataforge.exceptions.StorageException;
import hep.dataforge.meta.Annotated;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.storage.api.PointLoader;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.commons.LoaderFactory;
import hep.dataforge.values.Value;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A device controller for Dubna PKT 8 cryogenic thermometry device
 *
 * @author Alexander Nozik
 */
public class PKT8Device extends DataDevice<PKT8Device.PKT8Measurement> implements PortHandler.PortController {

    private static final String[] CHANNEL_DESIGNATIONS = {"a", "b", "c", "d", "e", "f", "g", "h"};

    private PointLoader pointLoader;
    private RegularPointCollector collector;

    private boolean isStarted = false;
    private PortHandler handler;
    private int sps = -1;
    private int pga = -1;
    private int abuf = -1;

    /**
     * The key is the letter (a,b,c,d...) as in measurements
     */
    private final Map<String, PKT8Channel> channels = new HashMap<>();

    public PKT8Device(String name, Context context, Meta annotation) {
        super(name, context, annotation);
    }

    /**
     * Start measurement
     *
     * @throws ControlException
     */
    @Override
    public void doStart(Meta measurement) throws ControlException {

//        Meta meta = new MetaChain("device", measurement.meta(),meta());
        if (!isStarted) {
            //setup storage

            try {
                Storage storage = getPrimaryStorage(measurement);
                String suffix = Integer.toString((int) Instant.now().toEpochMilli());

                // Building data format
                FormatBuilder formatBuilder = new FormatBuilder()
                        .addTime("timestamp");
                List<String> names = new ArrayList<>();

                for (PKT8Channel channel : this.channels.values()) {
                    formatBuilder.addNumber(channel.getName());
                    names.add(channel.getName());
                }

                this.pointLoader = LoaderFactory.buildPointLoder(storage, "cryotemp_" + suffix, "", "timestamp", formatBuilder.build());

                Duration duration = Duration.parse(meta().getString("averagingDuration", "PT30S"));

                collector = new RegularPointCollector((dp) -> {
                    if (pointLoader != null) {
                        try {
                            getLogger().debug("Point measurement complete. Pushing...");
                            pointLoader.push(dp);
                        } catch (StorageException ex) {
                            getLogger().error("Error while pushing point to loader", ex);
                        }
                    }
                }, duration, names);
            } catch (StorageException ex) {
                getLogger().error("Can't setup storage", ex);
            }

            handler.send("s");
            isStarted = true;
        }
    }

    @Override
    public void init() throws ControlException {

        //read channel configuration
        if (meta().hasNode("channel")) {
            for (Meta node : meta().getNodes("channel")) {
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

        //setup connection
        if (meta().hasNode("debug")) {
            handler = new PKT8VirtualPort("PKT8", meta().getNode("debug"));
        } else {
            String ip = this.meta().getString("connection.ip", "127.0.0.1");
            int port = this.meta().getInt("connection.port", 4001);
            handler = new TcpPortHandler(ip, port, getName());
        }
        handler.setDelimeter("\n");
        handler.holdBy(this);
        handler.open();
        handler.send("p");
        handler.sendAndWait("p", null, 1000);

        //update parameters from meta
        if (meta().hasValue("pga")) {
            getLogger().info("Setting dynamic range to " + meta().getInt("pga"));
            String response = handler.sendAndWait("g" + meta().getInt("pga"), null, 400).trim();
            if (response.contains("=")) {
                this.pga = Integer.parseInt(response.substring(4));
            } else {
                getLogger().error("Setting pga failsed with message: " + response);
            }
        }

        setSPS(meta().getInt("sps", 0));
        setBUF(meta().getInt("abuf", 100));

        super.init();
    }

    @Override
    public void shutdown() throws ControlException {
        stop();
        try {
            this.handler.unholdBy(this);
            this.handler.close();

        } catch (Exception ex) {
            throw new ControlException(ex);
        }
        super.shutdown();
    }

    private void setSPS(int sps) throws PortException {
        getLogger().info("Setting sampling rate to " + spsToStr(sps));
        String response = handler.sendAndWait("v" + sps, null, 400).trim();
        if (response.contains("=")) {
            this.sps = Integer.parseInt(response.substring(4));
            getLogger().info("successfully sampling rate to {}", spsToStr(this.sps));
        } else {
            getLogger().error("Setting sps failsed with message: " + response);
        }
    }

    public Collection<PKT8Channel> getChanels() {
        return this.channels.values();
    }

    private void setBUF(int buf) throws PortException {
        getLogger().info("Setting avaraging buffer size to " + buf);
        String response = handler.sendAndWait("b" + buf, null, 400).trim();
        if (response.contains("=")) {
            this.abuf = Integer.parseInt(response.substring(14));
            getLogger().info("successfully set buffer size to {}", this.abuf);
        } else {
            getLogger().error("Setting averaging buffer failsed with message: " + response);
        }
    }

    @Override
    public void doStop() throws ControlException {
        handler.send("p");
        isStarted = false;
        if (collector != null) {
            collector.cancel();
        }
    }

    public void changeParameters(int sps, int abuf) {
        this.executor.submit(() -> {
            try {
                stop();
                //setting sps
                setSPS(sps);
                //setting buffer
                setBUF(abuf);
                start();
            } catch (ControlException ex) {
                getLogger().error("Control error", ex);
            }
        });
    }

    @Override
    public void accept(String message) {
        String trimmed = message.trim();

        if (isStarted) {
            if (trimmed.equals("stopped")) {
                isStarted = false;
                getLogger().info("Measurement paused");
            } else {
                String designation = trimmed.substring(0, 1);
                double rawValue = Double.parseDouble(trimmed.substring(1)) / 100;

                if (channels.containsKey(designation)) {
                    PKT8Channel channel = channels.get(designation);
                    notifyMeasurementComplete(channel.getName(), rawValue, channel.getTemperature(rawValue));
                    collector.put(channel.getName(), channel.getTemperature(rawValue));
                } else {
                    notifyMeasurementComplete(designation, rawValue, -1);
                }

            }
        }
    }

    private void notifyMeasurementComplete(String channel, double rawValue, double temperature) {
        measurementResult(null, new PKT8Measurement(channel, rawValue, temperature));
    }

    @Override
    public void error(String errorMessage, Throwable error) {

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
                return "2,5 SPS";
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
     * @param sps
     * @return
     */
    private String pgaToStr(int sps) {
        switch (sps) {
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
                return "± 156,25 mV";
            case 6:
                return "± 78,125 mV";
            default:
                return "unknown value";
        }
    }

    public String getSPS() {
        return spsToStr(sps);
    }

    public String getPGA() {
        return pgaToStr(pga);
    }

    public String getABUF() {
        return Integer.toString(abuf);
    }

    public class PKT8Channel implements Named, Annotated {

        private final Meta meta;
        private final Function<Double, Double> transformation;

        public PKT8Channel(String name) {
            this.meta = new MetaBuilder("channel")
                    .putValue("name", name);
            transformation = (d) -> d;
        }

        public PKT8Channel(Meta meta) {
            this.meta = meta;

            String transformationType = meta.getString("transformationType", "default");
            if (meta.hasValue("coefs")) {
                switch (transformationType) {
                    case "default":
                    case "hyperbolic":
                        List<Value> coefs = meta.getValue("coefs").listValue();
                        double r0 = meta.getDouble("r0", 1000);
                        transformation = (r) -> {
                            if (coefs == null) {
                                return -1d;
                            } else {
                                double res = 0;
                                for (int i = 0; i < coefs.size(); i++) {
                                    res += coefs.get(i).doubleValue() * Math.pow(r0 / r, i);
                                }
                                return res;
                            }
                        };
                        break;
                    default:
                        throw new RuntimeException("Unknown transformation type");
                }
            } else {
                //identity transformation
                transformation = (d) -> d;

            }

        }

        @Override
        public String getName() {
            return meta().getString("name");
        }

        @Override
        public Meta meta() {
            return meta;
        }

        public String description() {
            return meta().getString("description", "");
        }

        /**
         *
         * @param r negative if temperature transformation not defined
         * @return
         */
        public double getTemperature(double r) {
            return transformation.apply(r);
        }

    }

    public static class PKT8Measurement {

        public String channel;
        public double rawValue;
        public double temperature;

        public PKT8Measurement(String channel, double rawValue, double temperature) {
            this.channel = channel;
            this.rawValue = rawValue;
            this.temperature = temperature;
        }

    }
}
