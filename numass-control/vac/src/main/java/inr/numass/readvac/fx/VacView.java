/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.fx;

import hep.dataforge.control.devices.DeviceListener;
import hep.dataforge.control.measurements.MeasurementListener;

/**
 *
 * @author Alexander Nozik
 */
public interface VacView extends DeviceListener, MeasurementListener<Double> {
    
}
