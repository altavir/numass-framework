/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.devices;

import hep.dataforge.control.collectors.PointCollector;
import hep.dataforge.control.collectors.ValueCollector;
import hep.dataforge.control.measurements.AbstractMeasurement;
import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.control.measurements.Sensor;
import hep.dataforge.points.DataPoint;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.exceptions.MeasurementException;
import hep.dataforge.values.Value;
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
public class VacCollectorDevice extends Sensor<DataPoint> {

    private Map<String, Sensor> sensorMap = new HashMap<>();

    public void setSensors(Sensor... sensors) {
        sensorMap = new LinkedHashMap<>(sensors.length);
        for (Sensor sensor : sensors) {
            sensorMap.put(sensor.getName(), sensor);
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

        private final ValueCollector collector = new PointCollector(this::onResult, sensorMap.keySet());
        private ScheduledExecutorService executor;
        private ScheduledFuture<?> currentTask;

        @Override
        public void start() {
            executor = Executors
                    .newSingleThreadScheduledExecutor((Runnable r) -> new Thread(r, "VacuumMeasurement thread"));
            currentTask = executor.scheduleWithFixedDelay(() -> {
                sensorMap.entrySet().stream().parallel().forEach((entry) -> {
                    try {
                        Object value = entry.getValue().read();
                        collector.put(entry.getKey(), value);
                    } catch (Exception ex) {
                        onError(ex);
                        collector.put(entry.getKey(), Value.NULL);
                    }
                });
            }, 0, meta().getInt("delay", 5000), TimeUnit.MILLISECONDS);
        }

        @Override
        public boolean stop(boolean force) {
            boolean isRunning = currentTask != null;
            if (isRunning) {
                getLogger().debug("Stoping vacuum collector measurement");
                currentTask.cancel(force);
                executor.shutdown();
                currentTask = null;
                onFinish();
            }
            return isRunning;
        }
    }

}
