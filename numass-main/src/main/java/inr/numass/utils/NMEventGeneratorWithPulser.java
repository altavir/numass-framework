package inr.numass.utils;

import hep.dataforge.meta.Meta;
import inr.numass.storage.NMEvent;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * Created by darksnake on 25-Nov-16.
 */
public class NMEventGeneratorWithPulser extends NMEventGenerator {
    private RealDistribution pulserChanelDistribution;
    private double pulserDist;
    private NMEvent pulserEvent;
    private NMEvent nextEvent;

    public NMEventGeneratorWithPulser(RandomGenerator rnd, Meta meta) {
        super(rnd, meta);
        pulserChanelDistribution = new NormalDistribution(
                meta.getDouble("pulser.mean", 3450),
                meta.getDouble("pulser.sigma", 86.45)
        );
        pulserDist = 1d / meta.getDouble("pulser.freq", 66.43);
        pulserEvent = generatePulserEvent();
    }

    @Override
    public synchronized NMEvent get() {
        //expected next event
        if(nextEvent == null ){
            nextEvent = nextEvent(prevEvent);
        }
        //if pulser event is first, then leave next event as is and return pulser event
        if (pulserEvent.getTime() < nextEvent.getTime()) {
            NMEvent res = pulserEvent;
            pulserEvent = generatePulserEvent();
            return res;
        } else {
            //else return saved next event and generate next one
            prevEvent = nextEvent;
            nextEvent = nextEvent(prevEvent);
            return prevEvent;
        }
    }

    private NMEvent generatePulserEvent() {
        short channel = (short) pulserChanelDistribution.sample();
        double time;
        if (pulserEvent == null) {
            time = rnd.nextDouble() * pulserDist;
        } else {
            time = pulserEvent.getTime() + pulserDist;
        }
        return new NMEvent(channel, time);
    }
}
