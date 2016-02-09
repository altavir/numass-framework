/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.devices;

import hep.dataforge.context.Context;
import hep.dataforge.control.collectors.PointCollector;
import hep.dataforge.control.collectors.ValueCollector;
import hep.dataforge.control.measurements.AbstractMeasurement;
import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.control.measurements.Sensor;
import hep.dataforge.data.DataPoint;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.exceptions.MeasurementException;
import hep.dataforge.meta.Meta;
import hep.dataforge.values.Value;
import java.util.Collection;
import java.util.HashMap;
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

    private final Map<String, Sensor<Double>> sensorMap;

    public VacCollectorDevice(String name, Context context, Meta meta, Sensor<Double>... sensors) {
        super(name, context, meta);
        sensorMap = new HashMap<>(sensors.length);
        for (Sensor<Double> sensor : sensors) {
            sensorMap.put(sensor.getName(), sensor);
        }
        //TODO add automatic construction from meta using deviceManager
    }

    @Override
    protected Object calculateState(String stateName) throws ControlException {
        //TODO add dot path notation for states
        return Value.NULL;
    }

    @Override
    protected Measurement<DataPoint> createMeasurement() {
        return new VacuumMeasurement();
    }

    @Override
    public String type() {
        return "Numass vacuum";
    }
    
    public Collection<Sensor<Double>> getSensors(){
        return sensorMap.values();
    }

    private class VacuumMeasurement extends AbstractMeasurement<DataPoint> {

        private final ValueCollector collector = new PointCollector(this::onResult, sensorMap.keySet());
        private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        private ScheduledFuture<?> currentTask;

        @Override
        public void start() {
            currentTask = executor.scheduleWithFixedDelay(() -> {
                sensorMap.entrySet().stream().parallel().forEach((entry) -> {
                    try {
                        collector.put(entry.getKey(), entry.getValue().getMeasurement().getResult());
                    } catch (MeasurementException ex) {
                        onError(ex);
                        collector.put(entry.getKey(), Value.NULL);
                    }
                });
            }, 0, getDelay(), TimeUnit.MILLISECONDS);
        }

        private int getDelay() {
            return meta().getInt("delay", 5000);
        }

        @Override
        public boolean stop(boolean force) {
            boolean isRunning = currentTask != null;
            if (isRunning) {
                currentTask.cancel(force);
                isFinished = true;
                currentTask = null;
            }
            return isRunning;
        }
    }
    
}
