/* 
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package inr.numass.utils;

import hep.dataforge.meta.Meta;
import inr.numass.storage.NMEvent;
import inr.numass.storage.NMPoint;
import inr.numass.storage.RawNMPoint;
import org.apache.commons.math3.distribution.EnumeratedRealDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A generator for Numass events with given energy spectrum
 *
 * @author Darksnake
 */
public class NMEventGenerator implements Supplier<NMEvent> {

    protected final RandomGenerator rnd;
    protected double cr;
    protected RealDistribution distribution;
    protected NMEvent prevEvent;

    public NMEventGenerator(RandomGenerator rnd, double cr) {
        this.cr = cr;
        this.rnd = rnd;
    }

    public NMEventGenerator(RandomGenerator rnd, Meta meta) {
        this.cr = meta.getDouble("cr");
        this.rnd = rnd;
    }

    public void loadSpectrum(RawNMPoint point, int minChanel, int maxChanel) {
        List<Short> shorts = new ArrayList<>();
        point.getEvents().stream()
                .filter((event) -> ((event.getChanel() > minChanel) && (event.getChanel() < maxChanel)))
                .forEach((event) -> shorts.add(event.getChanel()));
        double[] doubles = new double[shorts.size()];

        for (int i = 0; i < shorts.size(); i++) {
            doubles[i] = shorts.get(i);
        }

        EmpiricalDistribution d = new EmpiricalDistribution();
        d.load(doubles);

        distribution = d;
    }

    public void loadSpectrum(Map<Double, Double> spectrum) {
        double[] chanels = new double[spectrum.size()];
        double[] values = new double[spectrum.size()];
        int i = 0;
        for (Map.Entry<Double, Double> entry : spectrum.entrySet()) {
            chanels[i] = entry.getKey();
            values[i] = entry.getValue();
        }
        distribution = new EnumeratedRealDistribution(chanels, values);
    }

    public void loadSpectrum(double[] channels, double[] values) {
        distribution = new EnumeratedRealDistribution(channels, values);
    }

    public void loadSpectrum(NMPoint point) {
        double[] chanels = new double[RawNMPoint.MAX_CHANEL];
        double[] values = new double[RawNMPoint.MAX_CHANEL];
        for (int i = 0; i < RawNMPoint.MAX_CHANEL; i++) {
            chanels[i] = i;
            values[i] = point.getCountInChanel(i);
        }
        distribution = new EnumeratedRealDistribution(chanels, values);
    }

    public void loadSpectrum(NMPoint point, NMPoint reference) {
        double[] chanels = new double[RawNMPoint.MAX_CHANEL];
        double[] values = new double[RawNMPoint.MAX_CHANEL];
        for (int i = 0; i < RawNMPoint.MAX_CHANEL; i++) {
            chanels[i] = i;
            values[i] = Math.max(0, point.getCountInChanel(i) - reference.getCountInChanel(i));
        }
        distribution = new EnumeratedRealDistribution(chanels, values);
    }

    /**
     * @param point
     * @param reference
     * @param lower     lower channel for spectrum generation
     * @param upper     upper channel for spectrum generation
     */
    public void loadSpectrum(NMPoint point, NMPoint reference, int lower, int upper) {
        double[] chanels = new double[RawNMPoint.MAX_CHANEL];
        double[] values = new double[RawNMPoint.MAX_CHANEL];
        for (int i = lower; i < upper; i++) {
            chanels[i] = i;
            values[i] = Math.max(0, point.getCountInChanel(i) - (reference == null ? 0 : reference.getCountInChanel(i)));
        }
        distribution = new EnumeratedRealDistribution(chanels, values);
    }


    protected NMEvent nextEvent(NMEvent prev) {
        short chanel;

        if (distribution != null) {
            chanel = (short) distribution.sample();
        } else {
            chanel = 1600;
        }

        return new NMEvent(chanel, (prev == null ? 0 : prev.getTime()) + nextExpDecay(1d / cr));
    }


    @Override
    public synchronized NMEvent get() {
        return prevEvent = nextEvent(prevEvent);
    }

    private double nextExpDecay(double mean) {
        return -mean * Math.log(1 - rnd.nextDouble());
    }

}
