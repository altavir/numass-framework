/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.utils;

import hep.dataforge.tables.DataPoint;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.MapPoint;
import hep.dataforge.tables.Table;
import inr.numass.data.NumassPoint;
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

    private static String[] pointNames = {"U", "amp", "expConst", "correction"};

//    private final static int CUTOFF = -200;

//    public double get(Logable log, Meta meta, NMPoint point) {
//        if (point.getVoltage() >= meta.getDouble("underflow.threshold", 17000)) {
//            if (meta.hasValue("underflow.function")) {
//                return TritiumUtils.pointExpression(meta.getString("underflow.function"), point);
//            } else {
//                return 1;
//            }
//        } else {
//            try {
//                int xLow = meta.getInt("underflow.lowerBorder", meta.getInt("lowerWindow"));
//                int xHigh = meta.getInt("underflow.upperBorder", 800);
//                int binning = meta.getInt("underflow.binning", 20);
//                int upper = meta.getInt("upperWindow", RawNMPoint.MAX_CHANEL - 1);
//                long norm = point.getCountInWindow(xLow, upper);
//                double[] fitRes = getUnderflowExpParameters(point, xLow, xHigh, binning);
//                double correction = fitRes[0] * fitRes[1] * (Math.exp(xLow / fitRes[1]) - 1d) / norm + 1d;
//                return correction;
//            } catch (Exception ex) {
//                log.reportError("Failed to calculate underflow parameters for point {} with message:", point.getVoltage(), ex.getMessage());
//                return 1d;
//            }
//        }
//    }

//    public Table fitAllPoints(Iterable<NMPoint> data, int xLow, int xHigh, int binning) {
//        ListTable.Builder builder = new ListTable.Builder("U", "amp", "expConst");
//        for (NMPoint point : data) {
//            double[] fitRes = getUnderflowExpParameters(point, xLow, xHigh, binning);
//            builder.row(point.getVoltage(), fitRes[0], fitRes[1]);
//        }
//        return builder.build();
//    }

    public DataPoint fitPoint(NumassPoint point, int xLow, int xHigh, int upper, int binning) {
        double norm = ((double) point.getCountInWindow(xLow, upper)) / point.getLength();
        double[] fitRes = getUnderflowExpParameters(point, xLow, xHigh, binning);
        double a = fitRes[0];
        double sigma = fitRes[1];

        return new MapPoint(pointNames, point.getVoltage(), a, sigma, a * sigma * Math.exp(xLow / sigma) / norm + 1d);
    }

    public Table fitAllPoints(Iterable<NumassPoint> data, int xLow, int xHigh, int upper, int binning) {
        ListTable.Builder builder = new ListTable.Builder(pointNames);
        for (NumassPoint point : data) {
            builder.row(fitPoint(point,xLow,xHigh,upper,binning));
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
    private double[] getUnderflowExpParameters(NumassPoint point, int xLow, int xHigh, int binning) {
        try {
            if (xHigh <= xLow) {
                throw new IllegalArgumentException("Wrong borders for underflow calculation");
            }
            List<WeightedObservedPoint> points = point.getMap(binning, false)
                    .entrySet().stream()
                    .filter(entry -> entry.getKey() >= xLow && entry.getKey() <= xHigh)
                    .map(p -> new WeightedObservedPoint(
                            1d,//1d / p.getValue() , //weight
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
            double sigma = parameters[1];
            //return a * (Math.exp(x / sigma) - 1);
            return a * Math.exp(x / sigma);
        }

        @Override
        public double[] gradient(double x, double... parameters) {
            if (parameters.length != 2) {
                throw new DimensionMismatchException(parameters.length, 2);
            }
            double a = parameters[0];
            double sigma = parameters[1];
            return new double[]{
                    Math.exp(x / sigma),
                    -a * x / sigma / sigma * Math.exp(x / sigma)
            };
        }

    }

}
