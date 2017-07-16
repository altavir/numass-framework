package inr.numass.data

import groovy.transform.CompileStatic
import hep.dataforge.grind.Grind
import hep.dataforge.maths.histogram.Histogram
import hep.dataforge.maths.histogram.UnivariateHistogram
import inr.numass.data.analyzers.TimeAnalyzer
import inr.numass.data.api.NumassBlock

import java.util.stream.DoubleStream

/**
 * Created by darksnake on 27-Jun-17.
 */
@CompileStatic
class PointAnalyzer {

    static TimeAnalyzer analyzer = new TimeAnalyzer();

    static Histogram histogram(NumassBlock point, int loChannel = 0, int upChannel = 4000, double binSize = 1e-6d, int binNum = 500) {
        return UnivariateHistogram.buildUniform(0d, binSize * binNum, binSize)
                .fill(analyzer.timeChain(point, Grind.buildMeta("window.lo": loChannel, "window.up": upChannel)))
    }

    static Histogram histogram(DoubleStream stream, double binSize = 1e-6d, int binNum = 500) {
        return UnivariateHistogram.buildUniform(0d, binSize * binNum, binSize).fill(stream)
    }

    static class Result {
        double cr;
        double crErr;
        long num;
        double t0;
        int loChannel;
        int upChannel;
    }
}
