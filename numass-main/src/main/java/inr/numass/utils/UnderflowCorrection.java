/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.utils;

import hep.dataforge.io.reports.Logable;
import hep.dataforge.meta.Meta;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.Table;
import inr.numass.storage.NMPoint;
import inr.numass.storage.NumassData;
import inr.numass.storage.RawNMPoint;
import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.fitting.SimpleCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A class to calculate underflow correction
 *
 * @author <a href="mailto:altavir@gmail.com">Alexander Nozik</a>
 */
public class UnderflowCorrection {

    public double get(Logable log, Meta meta, NMPoint point) {
        if (point.getUset() >= meta.getDouble("underflow.threshold", 17000)) {
            if (meta.hasValue("underflow.function")) {
                return TritiumUtils.pointExpression(meta.getString("underflow.function"), point);
            } else {
                return 1;
            }
        } else {
            try {
                int xLow = meta.getInt("underflow.lowerBorder", meta.getInt("lowerWindow"));
                int xHigh = meta.getInt("underflow.upperBorder", 800);
                int binning = meta.getInt("underflow.binning", 20);
                int upper = meta.getInt("upperWindow", RawNMPoint.MAX_CHANEL - 1);
                long norm = point.getCountInWindow(xLow, upper);
                double[] fitRes = getUnderflowExpParameters(point, xLow, xHigh, binning);
                double correction = fitRes[0] * fitRes[1] * (Math.exp(xLow / fitRes[1]) - 1d) / norm + 1d;
                return correction;
            } catch (Exception ex) {
                log.reportError("Failed to calculate underflow parameters for point {} with message:", point.getUset(), ex.getMessage());
                return 1d;
            }
        }
    }

    public Table fitAllPoints(NumassData data, int xLow, int xHigh, int binning) {
        ListTable.Builder builder = new ListTable.Builder("U", "amp", "expConst");
        for (NMPoint point : data.getNMPoints()) {
            double[] fitRes = getUnderflowExpParameters(point, xLow, xHigh, binning);
            builder.row(point.getUset(), fitRes[0], fitRes[1]);
        }
        return builder.build();
    }

    public Table fitAllPoints(NumassData data, int xLow, int xHigh, int upper, int binning) {
        ListTable.Builder builder = new ListTable.Builder("U", "amp", "expConst", "correction");
        for (NMPoint point : data.getNMPoints()) {
            double norm = ((double) point.getCountInWindow(xLow, upper))/point.getLength();
            double[] fitRes = getUnderflowExpParameters(point, xLow, xHigh, binning);
            builder.row(point.getUset(), fitRes[0], fitRes[1], fitRes[0] * fitRes[1] * (Math.exp(xLow / fitRes[1]) - 1d) / norm + 1d);
        }
        return builder.build();
    }

    /**
     * Calculate underflow exponent parameters using (xLow, xHigh) window for
     * extrapolation
     *
     * @param point
     * @param xLow
     * @param xHigh
     * @return
     */
    private double[] getUnderflowExpParameters(NMPoint point, int xLow, int xHigh, int binning) {
        try {
            if (xHigh <= xLow) {
                throw new IllegalArgumentException("Wrong borders for underflow calculation");
            }
            List<WeightedObservedPoint> points = point.getMapWithBinning(binning, false)
                    .entrySet().stream()
                    .filter(entry -> entry.getKey() >= xLow && entry.getKey() <= xHigh)
                    .map(p -> new WeightedObservedPoint(
                            1d / p.getValue() * point.getLength() * point.getLength(), //weight
                            p.getKey(), // x
                            p.getValue() / binning / point.getLength()) //y
                    )
                    .collect(Collectors.toList());
            SimpleCurveFitter fitter = SimpleCurveFitter.create(new ExponentFunction(), new double[]{1d, 200d});
            return fitter.fit(points);
        } catch (Exception ex) {
            return new double[]{0, 0};
        }
    }

    /**
     * Exponential function for fitting
     */
    private static class ExponentFunction implements ParametricUnivariateFunction {

        @Override
        public double value(double x, double... parameters) {
            if (parameters.length != 2) {
                throw new DimensionMismatchException(parameters.length, 2);
            }
            double a = parameters[0];
            double x0 = parameters[1];
            return a * Math.exp(x / x0);
        }

        @Override
        public double[] gradient(double x, double... parameters) {
            if (parameters.length != 2) {
                throw new DimensionMismatchException(parameters.length, 2);
            }
            double a = parameters[0];
            double x0 = parameters[1];
            return new double[]{Math.exp(x / x0), -a * x / x0 / x0 * Math.exp(x / x0)};
        }

    }

}
