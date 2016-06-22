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
package inr.numass.generators;

import inr.numass.storage.NMEvent;
import inr.numass.storage.NMPoint;
import inr.numass.storage.RawNMPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.distribution.EnumeratedRealDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * A generator for Numass events with given energy spectrum
 *
 * @author Darksnake
 */
public class NMEventGenerator {

    double cr;
//    UnivariateFunction signalShape;
    RealDistribution distribution;

    private final RandomGenerator generator;

    public NMEventGenerator(double cr) {
        this.cr = cr;
        generator = new JDKRandomGenerator();
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

    public void loadSpectrum(Map<Double, Double> spectrum, int minChanel, int maxChanel) {
        assert minChanel > 0;
        assert maxChanel < RawNMPoint.MAX_CHANEL;

        double[] chanels = new double[spectrum.size()];
        double[] values = new double[spectrum.size()];
        int i = 0;
        for (Map.Entry<Double, Double> entry : spectrum.entrySet()) {
            chanels[i] = entry.getKey();
            values[i] = entry.getValue();
            i++;
        }
        distribution = new EnumeratedRealDistribution(chanels, values);
    }

    public void loadSpectrum(NMPoint point, int minChanel, int maxChanel) {
        assert minChanel > 0;
        assert maxChanel < RawNMPoint.MAX_CHANEL;

        double[] chanels = new double[RawNMPoint.MAX_CHANEL];
        double[] values = new double[RawNMPoint.MAX_CHANEL];
        for (int i = 0; i < RawNMPoint.MAX_CHANEL; i++) {
            chanels[i] = i;
            values[i] = point.getCountInChanel(i);
            i++;
        }
        distribution = new EnumeratedRealDistribution(chanels, values);
    }

    public NMEvent nextEvent(NMEvent prev) {
        short chanel;

        if (distribution != null) {
            chanel = (short) distribution.sample();
        } else {
            chanel = 1600;
        }

        return new NMEvent(chanel, prev.getTime() + nextExp(1 / cr));
    }

    double nextExp(double mean) {
        double rand = this.nextUniform();
        return -mean * Math.log(1 - rand);
    }

    double nextPositiveGaussian(double mean, double sigma) {
        double res = -1;
        while (res <= 0) {
            res = mean + generator.nextGaussian() * sigma;
        }
        return res;
    }

    double nextUniform() {
        return generator.nextDouble();
    }

    void setSeed(int seed) {
        generator.setSeed(seed);
    }

}
