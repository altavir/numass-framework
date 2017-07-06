/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.utils;

import inr.numass.data.NumassPoint;
import inr.numass.data.PointBuilders;
import inr.numass.data.RawNMPoint;
import inr.numass.data.events.NumassEvent;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static java.lang.Math.max;

/**
 * @author <a href="mailto:altavir@gmail.com">Alexander Nozik</a>
 */
public class PileUpSimulator {
    private final static double us = 1e-6;//microsecond
    private final double pointLength;
    private final RandomGenerator rnd;
    private final List<NumassEvent> generated = new ArrayList<>();
    private final List<NumassEvent> pileup = new ArrayList<>();
    private final List<NumassEvent> registered = new ArrayList<>();
    private Supplier<NumassEvent> generator;
    private double uSet = 0;
    private AtomicInteger doublePileup = new AtomicInteger(0);

    public PileUpSimulator(double length, RandomGenerator rnd, Supplier<NumassEvent> sup) {
        this.rnd = rnd;
        generator = sup;//new NMEventGenerator(countRate, rnd);
        this.pointLength = length;
    }

    public PileUpSimulator(double length, RandomGenerator rnd, double countRate) {
        this.rnd = rnd;
        generator = new NMEventGenerator(rnd, countRate);
        this.pointLength = length;
    }

    public PileUpSimulator withGenerator(Supplier<NumassEvent> sup){
        this.generator = sup;
        return this;
    }

    public PileUpSimulator withUset(double uset){
        this.uSet = uset;
        return this;
    }

    public NumassPoint generated() {
        return PointBuilders.readRawPoint(new RawNMPoint(uSet, generated, pointLength));
    }

    public NumassPoint registered() {
        return PointBuilders.readRawPoint(new RawNMPoint(uSet, registered, pointLength));
    }

    public NumassPoint pileup() {
        return PointBuilders.readRawPoint(new RawNMPoint(uSet, pileup, pointLength));
    }

    /**
     * The amplitude for pileup event
     *
     * @param x
     * @return
     */
    private short pileupChannel(double x, short prevChanel, short nextChanel) {
        assert x > 0;
        //эмпирическая формула для канала
        double coef = max(0, 0.99078 + 0.05098 * x - 0.45775 * x * x + 0.10962 * x * x * x);
        if (coef < 0 || coef > 1) {
            throw new Error();
        }

        return (short) (prevChanel + coef * nextChanel);
    }

    /**
     * pileup probability
     *
     * @param delay
     * @return
     */
    private boolean pileup(double delay) {
        double prob = 1d / (1d + Math.pow(delay / (2.5 + 0.2), 42.96));
        return random(prob);
    }

    /**
     * Probability for next event to register
     *
     * @param delay
     * @return
     */
    private boolean nextEventRegistered(short prevChanel, double delay) {
        double average = 6.76102 - 4.31897E-4 * prevChanel + 7.88429E-8 * prevChanel * prevChanel + 0.2;
        double prob = 1d - 1d / (1d + Math.pow(delay / average, 75.91));
        return random(prob);
    }

    private boolean random(double prob) {
        return rnd.nextDouble() <= prob;
    }

    public synchronized PileUpSimulator generate() {
        NumassEvent next;
        double lastRegisteredTime = 0; // Time of DAQ closing
        //flag that shows that previous event was pileup
        boolean pileupFlag = false;
        while (true) {
            next = generator.get();
            if (next.getTime() > pointLength) {
                break;
            }
            generated.add(next);
            //not counting double pileups
            if (generated.size() > 1) {
                double delay = (next.getTime() - lastRegisteredTime) / us; //time between events in microseconds
                if (nextEventRegistered(next.getChanel(), delay)) {
                    //just register new event
                    registered.add(next);
                    lastRegisteredTime = next.getTime();
                    pileupFlag = false;
                } else if (pileup(delay)) {
                    if(pileupFlag){
                        //increase double pileup stack
                        doublePileup.incrementAndGet();
                    } else {
                        //pileup event
                        short newChannel = pileupChannel(delay, next.getChanel(), next.getChanel());
                        NumassEvent newEvent = new NumassEvent(newChannel, next.getTime());
                        //replace already registered event by event with new channel
                        registered.remove(registered.size() - 1);
                        registered.add(newEvent);
                        pileup.add(newEvent);
                        //do not change DAQ close time
                        pileupFlag = true; // up the flag to avoid secondary pileup
                    }
                } else {
                    // second event not registered, DAQ closed
                    pileupFlag = false;
                }
            } else {
                //register first event
                registered.add(next);
                lastRegisteredTime = next.getTime();
            }
        }
        return this;
    }

}
