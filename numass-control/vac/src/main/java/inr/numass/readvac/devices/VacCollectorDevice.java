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
import hep.dataforge.meta.Meta;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javafx.util.Pair;

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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected Measurement<DataPoint> createMeasurement() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String type() {
        return "Numass vacuum";
    }

    private class VacuumMeasurement extends AbstractMeasurement<DataPoint> {
        
        ValueCollector collector = new PointCollector(this::result, sensorMap.keySet());

        @Override
        protected Pair<DataPoint, Instant> doGet() throws Exception {
            sensorMap.values().stream().parallel().forEach(action);
        }

        @Override
        public boolean isFinished() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void start() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean stop(boolean force) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

}
