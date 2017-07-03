package inr.numass.data

import groovy.transform.CompileStatic
import hep.dataforge.maths.histogram.Histogram
import hep.dataforge.maths.histogram.UnivariateHistogram

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
            if (t >= t0 && event.chanel <= upChannel && event.chanel >= loChannel) {
                totalN++
                totalT += t
            }
            lastEvent = event
        }
        double cr = 1d / (totalT / totalN - t0);
        return new Result(cr: cr, crErr: cr / Math.sqrt(totalN), num: totalN, t0: t0, loChannel: loChannel, upChannel: upChannel)
    }


    static Histogram histogram(RawNMPoint point, int loChannel = 0, int upChannel = 4000) {
        List<Double> ts = new ArrayList<>();
        NMEvent lastEvent = point.events[0];

        for (int i = 1; i < point.events.size(); i++) {
            NMEvent event = point.events[i];
            double t = event.time - lastEvent.time;
            if (t >= 0 && event.chanel <= upChannel && event.chanel >= loChannel) {
                ts << t
            }
            lastEvent = event
        }
        return UnivariateHistogram.buildUniform(0d, 5e-4, 1e-6).fill(ts.stream().mapToDouble { it })
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
