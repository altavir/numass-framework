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
import inr.numass.storage.RawNMPoint;
import java.util.ArrayList;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.SynchronizedRandomGenerator;

/**
 *
 * @author Darksnake
 */
public class BunchGenerator {

    private double bunchCr; // additional count rate in bunch
    private double bunchDist;// average distance between bunches
    private double bunchLength; // length of bunches
    private double cr; // count rate of normal events
    private ExpGenerator expGen;

//    private ExponentialDistribution expGen;
    public BunchGenerator(double cr, double bunchLength, double bunchDist, double bunchCr) {
        this.cr = cr;
        this.bunchLength = bunchLength;
        this.bunchDist = bunchDist;
        this.bunchCr = bunchCr;
        expGen = new ExpGenerator(new SynchronizedRandomGenerator(new MersenneTwister()));
    }

    public BunchGenerator(double cr, double bunchLength, double bunchDist, double bunchCr, RandomGenerator gen) {
        this.cr = cr;
        this.bunchLength = bunchLength;
        this.bunchDist = bunchDist;
        this.bunchCr = bunchCr;
        expGen = new ExpGenerator(gen);
    }

    public ArrayList<NMEvent> generate(double dist, double length, double timeShift, boolean isBunch) {
        ArrayList<NMEvent> res = new ArrayList<>();
        ArrayList<Double> events = generateEvents(dist, length);
        for (Double event : events) {
            if (event < length) {
                res.add(new NMEvent((short)0,event + timeShift));
//                if (isBunch) {
//                    res.add(new DebunchEvent(event + timeShift, 10));
//                } else {
//                    res.add(new DebunchEvent(event + timeShift));
//                }
                
            }
        }
        return res;
    }

    ArrayList<Double> generateEvents(double dist, double timeTotal) {
        ArrayList<Double> res = new ArrayList<>();
        double timeCount = 0;
        double delta;
        while (timeCount < timeTotal) {
            delta = expGen.nextExp(dist);
            timeCount += delta;
            if (timeCount < timeTotal) {
                res.add(timeCount);
            }
        }
        return res;
    }

    /**
     * Создает пачку с треугольным распределением
     *
     * @param dist
     * @param timeTotal
     * @return
     */
    ArrayList<Double> generateEventsTriangle(double dist, double timeTotal) {
        ArrayList<Double> res = new ArrayList<>();
        double timeCount = 0;
        double delta;
        while (timeCount < timeTotal) {
            delta = expGen.nextExp(dist * timeTotal / (timeTotal - timeCount));
            timeCount += delta;
            if (timeCount < timeTotal) {
                res.add(timeCount);
            }
        }
        return res;
    }

    public ArrayList<NMEvent> generateNormalEvents(double measurementTime) {
        return generate(1 / cr, measurementTime, 0, false);
    }

    public ArrayList<NMEvent> generateTriangle(double dist, double length, double timeShift, boolean isBunch) {
        ArrayList<NMEvent> res = new ArrayList<>();
        ArrayList<Double> events = generateEventsTriangle(dist, length);
        for (Double event : events) {
            if (event < length) {
                res.add(new NMEvent((short)0,event + timeShift));                  
//                if (isBunch) {
//                    res.add(new DebunchEvent(event + timeShift, 10));
//                } else {
//                    res.add(new DebunchEvent(event + timeShift));
//                }

            }
        }
        return res;
    }

    /**
     *
     * @param measurementTime - total measurement time
     * @return
     */
    public RawNMPoint generateWithBunches(double measurementTime) {
        ArrayList<NMEvent> res = generateNormalEvents(measurementTime);
        ArrayList<Double> bunchList = generateEvents(bunchDist, measurementTime);
        for (Double bunchPos : bunchList) {
            res.addAll(generate(1 / bunchCr, bunchLength, bunchPos, true));
        }
        return new RawNMPoint(0, res, measurementTime);
    }

    /**
     *
     * @param measurementTime - total measurement time
     * @return
     */
    public RawNMPoint generateWithRandomBunches(double measurementTime) {
        ArrayList<NMEvent> res = generateNormalEvents(measurementTime);
        ArrayList<Double> bunchList = generateEvents(bunchDist, measurementTime);
        for (Double bunchPos : bunchList) {
            double l = expGen.nextSafeGaussian(bunchLength, bunchLength / 3);
            double lambda = expGen.nextSafeGaussian(1 / bunchCr, 1 / bunchCr / 3);
            res.addAll(generate(lambda, l, bunchPos, true));
        }
        return new RawNMPoint(0, res, measurementTime);
    }

    public RawNMPoint generateWithTriangleBunches(double measurementTime) {
        ArrayList<NMEvent> res = generateNormalEvents(measurementTime);
        ArrayList<Double> bunchList = generateEvents(bunchDist, measurementTime);
        for (Double bunchPos : bunchList) {
            res.addAll(generateTriangle(1 / bunchCr, bunchLength, bunchPos, true));
        }
        return new RawNMPoint(0, res, measurementTime);
    }

    public void setSeed(int seed) {
        this.expGen.setSeed(seed);
    }

    private static class ExpGenerator {

        private final RandomGenerator generator;

        public ExpGenerator(RandomGenerator generator) {
            this.generator = generator;
        }

        public ExpGenerator(RandomGenerator generator, int seed) {
            this.generator = generator;
            this.generator.setSeed(seed);
        }

        void setSeed(int seed) {
            generator.setSeed(seed);
        }

        double nextUniform() {
            return generator.nextDouble();
        }

        double nextExp(double mean) {
            double rand = this.nextUniform();
            return -mean * Math.log(1 - rand);
        }

        double nextSafeGaussian(double mean, double sigma) {
            double res = -1;
            while (res <= 0) {
                res = mean + generator.nextGaussian() * sigma;
            }
            return res;
        }
    }
}
