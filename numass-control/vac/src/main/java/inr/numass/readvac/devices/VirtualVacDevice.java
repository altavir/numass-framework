/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.devices;

import hep.dataforge.context.Context;
import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.control.measurements.Sensor;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.meta.Meta;
import hep.dataforge.values.Value;

/**
 *
 * @author Alexander Nozik <altavir@gmail.com>
 */
public class VirtualVacDevice extends Sensor<Double> {

    public VirtualVacDevice(String name, Context context, Meta meta) {
        super(name, context, meta);
    }

    @Override
    protected Object calculateState(String stateName) throws ControlException {
        return Value.NULL;
    }

    @Override
    protected Measurement<Double> createMeasurement() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String type() {
        return "Virtual vacuumeter device";
    }
    
}
