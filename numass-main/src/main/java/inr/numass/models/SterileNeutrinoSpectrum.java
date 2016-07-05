/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.models;

import hep.dataforge.functions.AbstractParametricFunction;
import hep.dataforge.functions.ParametricFunction;
import hep.dataforge.values.NamedValueSet;
import inr.numass.NumassContext;
import org.apache.commons.math3.analysis.BivariateFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 * Compact all-in-one model for sterile neutrino spectrum
 * @author Alexander Nozik
 */
public class SterileNeutrinoSpectrum extends AbstractParametricFunction {

    private static final String[] list = {"X", "trap", "E0", "mnu2", "msterile2", "U2"};
    BetaSpectrum source;

    public SterileNeutrinoSpectrum() {
        super(list);
    }

//    /**
//     * Transmission function including detector backscattering
//     *
//     * @param u
//     * @param set
//     * @return
//     */
//    private BivariateFunction transmission(double u, NamedValueSet set) {
//
//    }
//
//    /**
//     * Loss function excluding trapping
//     *
//     * @param u
//     * @param set
//     * @return
//     */
//    private BivariateFunction loss(double u, NamedValueSet set) {
//
//    }
//
//    /**
//     * Trapping spectrum
//     *
//     * @param u
//     * @param set
//     * @return
//     */
//    private BivariateFunction trapping(double u, NamedValueSet set) {
//
//    }

    /**
     * Source spectrum including final states
     *
     * @return
     */
    private ParametricFunction source() {
        return source;
    }

    private BivariateFunction convolute(BivariateFunction loss, BivariateFunction resolution) {
        return (eIn, u) -> {
            UnivariateFunction integrand = (double eOut) -> loss.value(eIn, eOut) * resolution.value(eOut, u);
            return NumassContext.defaultIntegrator.integrate(integrand, u, eIn);
        };
    }

    @Override
    public double derivValue(String parName, double u, NamedValueSet set) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double value(double u, NamedValueSet set) {
        return 0;
    }

    @Override
    public boolean providesDeriv(String name) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
