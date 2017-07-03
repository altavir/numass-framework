package inr.numass.data

import groovy.transform.CompileStatic
import hep.dataforge.maths.histogram.Histogram
import hep.dataforge.maths.histogram.UnivariateHistogram

import java.util.stream.DoubleStream

/**
 * Created by darksnake on 27-Jun-17.
 */
@CompileStatic
class PointAnalyzer {

    static Result analyzePoint(RawNMPoint point, double t0 = 0, int loChannel = 0, int upChannel = 4000) {
        int totalN = 0
        double totalT = 0;
        NMEvent lastEvent = point.events[0];

        for (int i = 1; i < point.events.size(); i++) {
            NMEvent event = point.events[i];
            double t = event.time - lastEvent.time;
            if (t < 0) {
                lastEvent = event
            } else if (t >= t0 && event.chanel <= upChannel && event.chanel >= loChannel) {
                totalN++
                totalT += t
                lastEvent = event
            }
        }
        double cr = 1d / (totalT / totalN - t0);
        return new Result(cr: cr, crErr: cr / Math.sqrt(totalN), num: totalN, t0: t0, loChannel: loChannel, upChannel: upChannel)
    }

    private static DoubleStream timeChain(RawNMPoint point, int loChannel = 0, int upChannel = 4000) {
        List<Double> ts = new ArrayList<>();
        NMEvent lastEvent = point.events[0];

        for (int i = 1; i < point.events.size(); i++) {
            NMEvent event = point.events[i];
            double t = event.time - lastEvent.time;
            if (t < 0) {
                lastEvent = event
            } else if (t >= 0 && event.chanel <= upChannel && event.chanel >= loChannel) {
                ts << t
                lastEvent = event
            }
        }
        return ts.stream().mapToDouble { it }
    }

    /**
     * Calculate the number of events in chain with delay and channel in given regions
     * @param point
     * @param t1
     * @param t2
     * @param loChannel
     * @param upChannel
     * @return
     */
    static long count(RawNMPoint point, double t1, double t2, int loChannel = 0, int upChannel = 4000) {
        return timeChain(point, loChannel, upChannel).filter { it > t1 && it < t2 }.count();
    }


    static Histogram histogram(RawNMPoint point, int loChannel = 0, int upChannel = 4000, double binSize = 1e-6d, int binNum = 500) {
        return UnivariateHistogram.buildUniform(0d, binSize*binNum, binSize).fill(timeChain(point, loChannel, upChannel))
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
