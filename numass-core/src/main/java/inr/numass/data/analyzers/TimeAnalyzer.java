package inr.numass.data.analyzers;

import hep.dataforge.meta.Meta;
import hep.dataforge.tables.ValueMap;
import hep.dataforge.values.Values;
import inr.numass.data.api.NumassBlock;
import inr.numass.data.api.NumassEvent;
import inr.numass.data.api.NumassPoint;
import inr.numass.data.api.SignalProcessor;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.DoubleStream;

/**
 * An analyzer which uses time information from events
 * Created by darksnake on 11.07.2017.
 */
public class TimeAnalyzer extends AbstractAnalyzer {

    public TimeAnalyzer(@Nullable SignalProcessor processor) {
        super(processor);
    }

    public TimeAnalyzer() {
    }

    @Override
    public Values analyze(NumassBlock block, Meta config) {
        int loChannel = config.getInt("window.lo", 0);
        int upChannel = config.getInt("window.up", Integer.MAX_VALUE);

        double t0 = config.getDouble("t0");

        AtomicLong totalN = new AtomicLong(0);
        AtomicReference<Double> totalT = new AtomicReference<>(0d);

        timeChain(block, config).forEach(delay -> {
            if (delay >= t0) {
                totalN.incrementAndGet();
                //TODO add progress listener here
                totalT.accumulateAndGet(delay, (d1, d2) -> d1 + d2);
            }
        });

        double countRate = 1d / (totalT.get() / totalN.get() - t0);
        double countRateError = countRate/Math.sqrt(totalN.get());
        long count = (long) (countRate * totalT.get());
        double length = totalT.get();

        if (block instanceof NumassPoint) {
            return new ValueMap(NAME_LIST_WITH_HV,
                    new Object[]{
                            ((NumassPoint) block).getVoltage(),
                            length,
                            count,
                            countRate,
                            countRateError,
                            new int[]{loChannel, upChannel},
                            block.getStartTime()
                    }
            );
        } else {
            return new ValueMap(NAME_LIST,
                    new Object[]{
                            length,
                            count,
                            countRate,
                            countRateError,
                            new int[]{loChannel, upChannel},
                            block.getStartTime()
                    }
            );
        }
    }

    public DoubleStream timeChain(NumassBlock block, Meta config) {
        AtomicReference<NumassEvent> lastEvent = new AtomicReference<>(null);
        return getEventStream(block, config).mapToDouble(event -> {
            if (lastEvent.get() == null) {
                lastEvent.set(event);
                return 0d;
            } else {
                double res = Duration.between(lastEvent.get().getTime(),event.getTime()).toNanos();//event.getTimeOffset() - lastEvent.get().getTimeOffset();
                if (res > 0) {
                    lastEvent.set(event);
                    return res;
                } else {
                    lastEvent.set(null);
                    return 0d;
                }
            }
        });
    }
}
