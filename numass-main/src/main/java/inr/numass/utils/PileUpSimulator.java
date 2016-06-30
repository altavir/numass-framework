/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.utils;

import inr.numass.storage.NMEvent;
import inr.numass.storage.NMPoint;
import inr.numass.storage.RawNMPoint;
import static java.lang.Math.max;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Alexander Nozik <altavir@gmail.com>
 */
public class PileUpSimulator {

    private final static double us = 1e-6;//microsecond

    private double uSet = 0;
    private final double pointLength;
    private final NMEventGenerator generator;
    private final List<NMEvent> generated = new ArrayList<>();
    private final List<NMEvent> pileup = new ArrayList<>();
    private final List<NMEvent> registred = new ArrayList<>();

    public PileUpSimulator(double countRate, double length) {
        generator = new NMEventGenerator(countRate);
        this.pointLength = length;
    }

    public PileUpSimulator withGenerator(NMPoint spectrum, NMPoint reference) {
        this.uSet = spectrum.getUset();
        generator.loadSpectrum(spectrum, reference);
        return this;
    }

    public PileUpSimulator withGenerator(NMPoint spectrum, NMPoint reference, int from, int to) {
        this.uSet = spectrum.getUset();
        generator.loadSpectrum(spectrum, reference, from, to);
        return this;
    }

    public PileUpSimulator withGenerator(NMPoint spectrum) {
        this.uSet = spectrum.getUset();
        generator.loadSpectrum(spectrum);
        return this;
    }

    public NMPoint generated() {
        return new NMPoint(new RawNMPoint(uSet, generated, pointLength));
    }

    public NMPoint registered() {
        return new NMPoint(new RawNMPoint(uSet, registred, pointLength));
    }

    public NMPoint pileup() {
        return new NMPoint(new RawNMPoint(uSet, pileup, pointLength));
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
        double prob = 1d / (1d + Math.pow(delay / 2.5, 42.96));
        return random(prob);
    }

    /**
     * Probability for next event to register
     *
     * @param delay
     * @return
     */
    private boolean nextEventRegistered(double delay) {
        double prob = 1d - 1d / (1d + Math.pow(delay / 6.2, 75.91));
        return random(prob);
    }

    private boolean random(double prob) {
        double r = generator.nextUniform();
        return r < prob;
    }

    public synchronized PileUpSimulator generate() {
        NMEvent current = null;
        boolean pileupFlag = false;
        while (true) {
            NMEvent next = generator.nextEvent(current);
            if (next.getTime() > pointLength) {
                break;
            }
            generated.add(next.clone());
            //flag that shows that previous event was pileup
            //not counting double pileups
            if (current != null) {
                double delay = (next.getTime() - current.getTime()) / us; //time between events in microseconds
                if (nextEventRegistered(delay)) {
                    //just register new event
                    registred.add(next.clone());
                    pileupFlag = false;
                } else if (pileup(delay) && !pileupFlag) {
                    //pileup event
                    short newChannel = pileupChannel(delay, current.getChanel(), next.getChanel());
                    NMEvent newEvent = new NMEvent(newChannel, current.getTime());
                    //replace already registered event by event with new channel
                    registred.remove(registred.size() - 1);
                    registred.add(newEvent.clone());
                    pileup.add(newEvent.clone());
                    pileupFlag = true; // up the flag to avoid secondary pileup
                } else {
                    // second event not registered
                    pileupFlag = false;
                }
            } else {
                //register first event
                registred.add(next.clone());
            }
            current = next;
        }
        return this;
    }

}
