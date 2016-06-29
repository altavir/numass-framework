/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.models;

import hep.dataforge.functions.AbstractParametricFunction;
import hep.dataforge.functions.ParametricFunction;
import hep.dataforge.values.NamedValueSet;

/**
 *
 * @author Alexander Nozik
 */
public class SterileNeutrinoSpectrum extends AbstractParametricFunction {
 
    private static final String[] list = {"X", "trap", "E0", "mnu2", "msterile2", "U2"};
    private ParametricFunction spectrum;

    public SterileNeutrinoSpectrum() {
        super(list);
    }
    

    @Override
    public double derivValue(String parName, double x, NamedValueSet set) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double value(double x, NamedValueSet set) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean providesDeriv(String name) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
