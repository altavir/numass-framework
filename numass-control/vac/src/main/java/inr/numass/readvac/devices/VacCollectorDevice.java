/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.devices;

import hep.dataforge.control.collectors.PointCollector;
import hep.dataforge.control.collectors.ValueCollector;
import hep.dataforge.control.connections.Roles;
import hep.dataforge.control.devices.annotations.RoleDef;
import hep.dataforge.control.measurements.AbstractMeasurement;
import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.control.measurements.Sensor;
import hep.dataforge.tables.DataPoint;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.exceptions.MeasurementException;
import hep.dataforge.tables.MapPoint;
import hep.dataforge.tables.PointListener;
import hep.dataforge.values.Value;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Alexander Nozik <altavir@gmail.com>
 */
@RoleDef(name = Roles.STORAGE_ROLE, objectType = PointListener.class, info = "Storage for acquired points")
public class VacCollectorDevice extends Sensor<DataPoint> {

    private Map<String, Sensor> sensorMap = new HashMap<>();

    public void setSensors(Sensor... sensors) {
        sensorMap = new LinkedHashMap<>(sensors.length);
        for (Sensor sensor : sensors) {
            sensorMap.put(sensor.getName(), sensor);
        }
    }

    public void setSensors(Iterable<Sensor> sensors) {
        sensorMap = new LinkedHashMap<>();
        for (Sensor sensor : sensors) {
            sensorMap.put(sensor.getName(), sensor);
        }
    }

    @Override
    public void init() throws ControlException {
        super.init();
        for (Sensor s : sensorMap.values()) {
            s.init();
        }
    }

    @Override
    protected Object calculateState(String stateName) throws ControlException {
        //TODO add dot path notation for states
        return Value.NULL;
    }

    @Override
    protected Measurement<DataPoint> createMeasurement() {
        //TODO use meta
        return new VacuumMeasurement();
    }

    @Override
    public String type() {
        return "Numass vacuum";
    }

    public void setDelay(int delay) throws MeasurementException {
        getConfig().setValue("delay", delay);
        if (isMeasuring()) {
            getMeasurement().stop(false);
            getMeasurement().start();
        }
    }

    @Override
    public void shutdown() throws ControlException {
        super.shutdown();
        for (Sensor sensor : getSensors()) {
            sensor.shutdown();
        }
    }

    public Collection<Sensor> getSensors() {
        return sensorMap.values();
    }

    private class VacuumMeasurement extends AbstractMeasurement<DataPoint> {

        private final ValueCollector collector = new PointCollector(this::result, sensorMap.keySet());
        private ScheduledExecutorService executor;
        private ScheduledFuture<?> currentTask;

        @Override
        public void start() {
            executor = Executors
                    .newSingleThreadScheduledExecutor((Runnable r) -> new Thread(r, "VacuumMeasurement thread"));
            currentTask = executor.scheduleWithFixedDelay(() -> {
                sensorMap.entrySet().stream().parallel().forEach((entry) -> {
                    try {
                        Object value;
                        if (entry.getValue().meta().getBoolean("disabled", false)) {
                            value = null;
                        } else {
                            value = entry.getValue().read();
                        }
                        collector.put(entry.getKey(), value);
                    } catch (Exception ex) {
                        collector.put(entry.getKey(), Value.NULL);
                    }
                });
            }, 0, meta().getInt("delay", 5000), TimeUnit.MILLISECONDS);
        }

        @Override
        protected synchronized void result(DataPoint result, Instant time) {
            super.result(result, time);
            forEachTypedConnection(Roles.STORAGE_ROLE, PointListener.class, (PointListener listener) -> {
                listener.accept(result);
            });
        }

        private DataPoint terminator() {
            MapPoint.Builder p = new MapPoint.Builder();
            p.putValue("timestamp", Instant.now());
            sensorMap.keySet().stream().forEach((n) -> {
                p.putValue(n, null);
            });
            return p.build();
        }

        @Override
        public boolean stop(boolean force) {
            boolean isRunning = currentTask != null;
            if (isRunning) {
                getLogger().debug("Stoping vacuum collector measurement. Writing terminator point");
                result(terminator());
                currentTask.cancel(force);
                executor.shutdown();
                currentTask = null;
                afterStop();
            }
            return isRunning;
        }
    }

}
