/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.maths;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.apache.commons.math3.util.Precision;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author Alexander Nozik
 */
public class Interpolation {

    public enum InterpolationType {
        LINE,
        SPLINE
    }

    /**
     *
     * @param values - input values as a map
     * @param loValue - value to be assumed for xs lower than lowest value in
     * the map. By default is assumed to be lowest value.
     * @param upValue - value to be assumed for xs higher than highest value in
     * the map. By default is assumed to be highest value.
     * @return
     */
    public static UnivariateFunction interpolate(Map<Number, Number> values, InterpolationType type, Double loValue, Double upValue) {
        SortedMap<Number, Number> sorted = new TreeMap<>();
        sorted.putAll(values);

        double lo = sorted.firstKey().doubleValue();
        double up = sorted.lastKey().doubleValue();
        double delta = 2 * Precision.EPSILON;

        double lval;
        if (loValue == null || Double.isNaN(loValue)) {
            lval = sorted.get(sorted.firstKey()).doubleValue();
        } else {
            lval = loValue;
        }

        double uval;
        if (upValue == null || Double.isNaN(upValue)) {
            uval = sorted.get(sorted.lastKey()).doubleValue();
        } else {
            uval = upValue;
        }

        UnivariateInterpolator interpolator;
        switch (type) {
            case SPLINE:
                interpolator = new SplineInterpolator();
                break;
            default:
                interpolator = new LinearInterpolator();
        }

        double[] xs = new double[sorted.size()];
        double[] ys = new double[sorted.size()];

        int i = 0;
        for (Map.Entry<Number, Number> entry : sorted.entrySet()) {
            xs[i] = entry.getKey().doubleValue();
            ys[i] = entry.getValue().doubleValue();
            i++;
        }
        
        UnivariateFunction interpolated = interpolator.interpolate(xs, ys);
        
        return (double x) -> {
            if (x < lo + delta) {
                return lval;
            } else if (x > up + delta) {
                return uval;
            } else {
                return interpolated.value(x);
            }
        };
    }
}
