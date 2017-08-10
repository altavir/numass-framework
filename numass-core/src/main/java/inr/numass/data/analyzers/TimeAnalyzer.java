package inr.numass.data.analyzers;

import hep.dataforge.meta.Meta;
import hep.dataforge.tables.ValueMap;
import hep.dataforge.values.Values;
import inr.numass.data.api.NumassBlock;
import inr.numass.data.api.NumassEvent;
import inr.numass.data.api.NumassPoint;
import inr.numass.data.api.SignalProcessor;
import javafx.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

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
        long t0 = getT0(block, config);

        AtomicLong totalN = new AtomicLong(0);
        AtomicLong totalT = new AtomicLong(0);

        getEventsWithDelay(block, config)
                .forEach(pair -> {
                    totalN.incrementAndGet();
                    //TODO add progress listener here
                    totalT.addAndGet(pair.getValue());
                });

        double countRate = 1e6 * totalN.get() / (totalT.get() / 1000 - t0 * totalN.get() / 1000);//1e9 / (totalT.get() / totalN.get() - t0);
        double countRateError = countRate / Math.sqrt(totalN.get());
        long count = (long) (totalT.get() * (countRate / 1e9));
        double length = totalT.get();

        if (block instanceof NumassPoint) {
            return ValueMap.of(NAME_LIST_WITH_HV,
                    ((NumassPoint) block).getVoltage(),
                    length,
                    count,
                    countRate,
                    countRateError,
                    new Integer[]{loChannel, upChannel},
                    block.getStartTime());
        } else {
            return ValueMap.of(NAME_LIST,
                    length,
                    count,
                    countRate,
                    countRateError,
                    new Integer[]{loChannel, upChannel},
                    block.getStartTime()
            );
        }
    }

    private long getT0(NumassBlock block, Meta config) {
        return config.getValue("t0",0).longValue();
    }

    /**
     * The chain of event times in nanos
     *
     * @param block
     * @param config
     * @return
     */
    public Stream<Pair<NumassEvent, Long>> getEventsWithDelay(NumassBlock block, Meta config) {
        long t0 = getT0(block, config);

        AtomicReference<NumassEvent> lastEvent = new AtomicReference<>(null);

        Stream<NumassEvent> eventStream = super.getEvents(block, config);//using super implementation

        return eventStream.map(event -> {
            if (lastEvent.get() == null) {
                lastEvent.set(event);
                return new Pair<>(event, 0L);
            } else {
                long res = event.getTimeOffset() - lastEvent.get().getTimeOffset();
                if (res >= 0) {
                    lastEvent.set(event);
                    return new Pair<>(event, res);
                } else {
                    lastEvent.set(null);
                    return new Pair<>(event, 0L);
                }
            }
        }).filter(pair -> pair.getValue() >= t0);
    }

    /**
     * The filtered stream of events
     * @param block
     * @param config
     * @return
     */
    @Override
    public Stream<NumassEvent> getEvents(NumassBlock block, Meta config) {
        return getEventsWithDelay(block, config).map(Pair::getKey);
    }
}
