/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.models.sterile;

import hep.dataforge.fitting.parametric.AbstractParametricBiFunction;
import hep.dataforge.meta.Meta;
import hep.dataforge.values.NamedValueSet;
import inr.numass.models.ResolutionFunction;
import static java.lang.Double.isNaN;
import static java.lang.Math.sqrt;
import org.apache.commons.math3.analysis.BivariateFunction;

/**
 *
 * @author Alexander Nozik <altavir@gmail.com>
 */
public class NumassResolution extends AbstractParametricBiFunction {

    private static final String[] list = {}; //leaving 

    private final double resA;
    private double resB = Double.NaN;
    private BivariateFunction tailFunction = ResolutionFunction.getConstantTail();

    public NumassResolution(Meta meta) {
        super(list);
        this.resA = meta.getDouble("A", 8.3e-5);
        this.resB = meta.getDouble("B", 0);
        if (meta.hasValue("tailAlpha")) {
            //add polinomial function here
            tailFunction = ResolutionFunction.getAngledTail(meta.getDouble("tailAlpha"), meta.getDouble("tailBeta", 0));
        } else {
            tailFunction = ResolutionFunction.getConstantTail();
        }
    }

    @Override
    public double derivValue(String parName, double x, double y, NamedValueSet set) {
        return 0;
    }

    private double getValueFast(double E, double U) {
        double delta = resA * E;
        if (E - U < 0) {
            return 0;
        } else if (E - U > delta) {
            return tailFunction.value(E, U);
        } else {
            return (E - U) / delta;
        }
    }

    @Override
    public boolean providesDeriv(String name) {
        return true;
    }

    @Override
    public double value(double E, double U, NamedValueSet set) {
        assert resA > 0;
        if (isNaN(resB)) {
            return this.getValueFast(E, U);
        }
        assert resB > 0;
        double delta = resA * E;
        if (E - U < 0) {
            return 0;
        } else if (E - U > delta) {
            return tailFunction.value(E, U);
        } else {
            return (1 - sqrt(1 - (E - U) / E * resB)) / (1 - sqrt(1 - resA * resB));
        }
    }

}
