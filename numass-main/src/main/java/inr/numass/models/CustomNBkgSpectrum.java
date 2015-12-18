/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.models;

import hep.dataforge.functions.ParametricFunction;
import hep.dataforge.maths.NamedDoubleSet;
import inr.numass.NumassContext;
import inr.numass.utils.TritiumUtils;
import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 * A spectrum with custom background, say tritium decays on walls
 *
 * @author Alexander Nozik
 */
public class CustomNBkgSpectrum extends NBkgSpectrum {
    
    public static CustomNBkgSpectrum tritiumBkgSpectrum(ParametricFunction source, double amplitude){
        UnivariateFunction differentialBkgFunction = TritiumUtils.tritiumBackgroundFunction(amplitude);
        UnivariateFunction integralBkgFunction = 
                (x) -> NumassContext.defaultIntegrator
                        .integrate(differentialBkgFunction, x, 18580d);
        return new CustomNBkgSpectrum(source, integralBkgFunction);
    }

    private UnivariateFunction customBackgroundFunction;

    public CustomNBkgSpectrum(ParametricFunction source) {
        super(source);
    }

    public CustomNBkgSpectrum(ParametricFunction source, UnivariateFunction customBackgroundFunction) {
        super(source);
        this.customBackgroundFunction = customBackgroundFunction;
    }

    @Override
    public double value(double x, NamedDoubleSet set) {
        if (customBackgroundFunction == null) {
            return super.value(x, set);
        } else {
            return super.value(x, set) + customBackgroundFunction.value(x);
        }
    }


}
