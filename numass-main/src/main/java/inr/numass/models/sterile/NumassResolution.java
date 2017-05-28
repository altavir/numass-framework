/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.models.sterile;

import hep.dataforge.context.Context;
import hep.dataforge.maths.MathPlugin;
import hep.dataforge.meta.Meta;
import hep.dataforge.stat.parametric.AbstractParametricBiFunction;
import hep.dataforge.values.NamedValueSet;
import inr.numass.models.ResolutionFunction;
import inr.numass.utils.ExpressionUtils;
import org.apache.commons.math3.analysis.BivariateFunction;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.sqrt;

/**
 * @author <a href="mailto:altavir@gmail.com">Alexander Nozik</a>
 */
public class NumassResolution extends AbstractParametricBiFunction {

    private static final String[] list = {}; //leaving 

    private final double resA;
    private double resB = 0;
    private BivariateFunction tailFunction = ResolutionFunction.getConstantTail();

    public NumassResolution(Context context, Meta meta) {
        super(list);
        this.resA = meta.getDouble("A", 8.3e-5);
        this.resB = meta.getDouble("B", 0);
        if (meta.hasValue("tail")) {
            String tailFunctionStr = meta.getString("tail");
            if (tailFunctionStr.startsWith("function::")) {
                tailFunction = MathPlugin.buildFrom(context).buildBivariateFunction(tailFunctionStr.substring(10));
            } else {
                tailFunction = (E, U) -> {
                    Map<String, Object> binding = new HashMap<>();
                    binding.put("E", E);
                    binding.put("U", U);
                    binding.put("D", E - U);
                    return ExpressionUtils.function(tailFunctionStr, binding);
                };
            }
        } else if (meta.hasValue("tailAlpha")) {
            //add polynomial function here
            double alpha = meta.getDouble("tailAlpha");
            double beta = meta.getDouble("tailBeta", 0);
            tailFunction = (double E, double U) -> 1 - (E - U) * (alpha + E / 1000d * beta) / 1000d;

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
        if (resB <= 0) {
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
