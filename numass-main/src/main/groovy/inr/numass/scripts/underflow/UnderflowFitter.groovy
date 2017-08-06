package inr.numass.scripts.underflow

import groovy.transform.CompileStatic
import hep.dataforge.tables.ListTable
import hep.dataforge.tables.Table
import hep.dataforge.tables.TableTransform
import hep.dataforge.tables.ValueMap
import hep.dataforge.values.Values
import inr.numass.data.NumassDataUtils
import org.apache.commons.math3.analysis.ParametricUnivariateFunction
import org.apache.commons.math3.exception.DimensionMismatchException
import org.apache.commons.math3.fitting.SimpleCurveFitter
import org.apache.commons.math3.fitting.WeightedObservedPoint

import java.util.stream.Collectors

import static inr.numass.data.api.NumassAnalyzer.CHANNEL_KEY
import static inr.numass.data.api.NumassAnalyzer.COUNT_RATE_KEY

@CompileStatic
class UnderflowFitter {

    private static String[] pointNames = ["U", "amp", "expConst", "correction"];

    static Values fitPoint(Double voltage, Table spectrum, int xLow, int xHigh, int upper, int binning) {

        double norm = spectrum.getRows().filter { Values row ->
            int channel = row.getInt(CHANNEL_KEY);
            return channel > xLow && channel < upper;
        }.mapToDouble { it.getValue(COUNT_RATE_KEY).doubleValue() }.sum();

        double[] fitRes = getUnderflowExpParameters(spectrum, xLow, xHigh, binning);
        double a = fitRes[0];
        double sigma = fitRes[1];

        return ValueMap.of(pointNames, voltage, a, sigma, a * sigma * Math.exp(xLow / sigma) / norm + 1d);
    }

    static Table fitAllPoints(Map<Double, Table> data, int xLow, int xHigh, int upper, int binning) {
        ListTable.Builder builder = new ListTable.Builder(pointNames);
        data.each { voltage, spectrum -> builder.row(fitPoint(voltage, spectrum, xLow, xHigh, upper, binning)) }
        return builder.build();
    }

    /**
     * Calculate underflow exponent parameters using (xLow, xHigh) window for
     * extrapolation
     *
     * @param xLow
     * @param xHigh
     * @return
     */
    private static double[] getUnderflowExpParameters(Table spectrum, int xLow, int xHigh, int binning) {
        try {
            if (xHigh <= xLow) {
                throw new IllegalArgumentException("Wrong borders for underflow calculation");
            }
            Table binned = TableTransform.filter(
                    NumassDataUtils.spectrumWithBinning(spectrum, binning),
                    CHANNEL_KEY,
                    xLow,
                    xHigh
            );

            List<WeightedObservedPoint> points = binned.getRows()
                    .map {
                new WeightedObservedPoint(
                        1d,//1d / p.getValue() , //weight
                        it.getDouble(CHANNEL_KEY), // x
                        it.getDouble(COUNT_RATE_KEY) / binning) //y
            }
            .collect(Collectors.toList());
            SimpleCurveFitter fitter = SimpleCurveFitter.create(new ExponentFunction(), [1d, 200d] as double[])
            return fitter.fit(points);
        } catch (Exception ex) {
            return [0, 0] as double[];
        }
    }

    /**
     * Exponential function for fitting
     */
    private static class ExponentFunction implements ParametricUnivariateFunction {
        @Override
        public double value(double x, double ... parameters) {
            if (parameters.length != 2) {
                throw new DimensionMismatchException(parameters.length, 2);
            }
            double a = parameters[0];
            double sigma = parameters[1];
            //return a * (Math.exp(x / sigma) - 1);
            return a * Math.exp(x / sigma);
        }

        @Override
        public double[] gradient(double x, double ... parameters) {
            if (parameters.length != 2) {
                throw new DimensionMismatchException(parameters.length, 2);
            }
            double a = parameters[0];
            double sigma = parameters[1];
            return [Math.exp(x / sigma), -a * x / sigma / sigma * Math.exp(x / sigma)] as double[]
        }

    }
}
