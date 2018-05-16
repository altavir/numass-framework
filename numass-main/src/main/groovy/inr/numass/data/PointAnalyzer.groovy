package inr.numass.data

import groovy.transform.CompileStatic
import hep.dataforge.grind.Grind
import hep.dataforge.maths.histogram.Histogram
import hep.dataforge.maths.histogram.UnivariateHistogram
import hep.dataforge.values.Values
import inr.numass.data.analyzers.TimeAnalyzer
import inr.numass.data.api.NumassBlock

import java.util.stream.LongStream

/**
 * Created by darksnake on 27-Jun-17.
 */
@CompileStatic
class PointAnalyzer {

    static final TimeAnalyzer analyzer = new TimeAnalyzer();

    static Histogram histogram(NumassBlock point, int loChannel = 0, int upChannel = 10000, double binSize = 0.5, int binNum = 500) {
        return UnivariateHistogram.buildUniform(0d, binSize * binNum, binSize)
                .fill(
                kotlin.streams.jdk8.StreamsKt.asStream(analyzer.getEventsWithDelay(point, Grind.buildMeta("window.lo": loChannel, "window.up": upChannel))).mapToDouble {
                    it.second / 1000 as double
                }
        )
    }

    static Histogram histogram(LongStream stream, double binSize = 0.5, int binNum = 500) {
        return UnivariateHistogram.buildUniform(0d, binSize * binNum, binSize).fill(stream.mapToDouble {
            it / 1000 as double
        })
    }

    static Values analyze(Map values = Collections.emptyMap(), NumassBlock block, Closure metaClosure = null) {
        return analyzer.analyze(block, Grind.buildMeta(values, metaClosure))
    }
//
//    static class Result {
//        double cr;
//        double crErr;
//        long num;
//        double t0;
//        int loChannel;
//        int upChannel;
//    }
}
