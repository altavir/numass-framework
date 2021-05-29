package hep.dataforge.maths

import hep.dataforge.maths.tables.GTable

import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Stream

/**
 * Created by darksnake on 13-Nov-16.
 */
class HistogramBuilder {
    private double[] binBorders;
    private Stream<Double> dataStream;

    GTable build(){
        List<Bin> bins = [];
        def min = binBorders[0]
        def max = binBorders[1];
        for(d in binBorders){
            bins << new Bin()
        }
    }


    private class Bin{
        double center;
        double min;
        double max;

        Bin(double min, double max) {
            this.min = min
            this.max = max
            center = (min + max)/2
        }
        private AtomicInteger counter = new AtomicInteger();

        def inc(){
            return counter.incrementAndGet();
        }

        def getCount(){
            return counter.intValue();
        }
    }
}
