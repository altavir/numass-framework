package inr.numass.data.analyzers;

import hep.dataforge.meta.Meta;
import hep.dataforge.tables.TableFormat;
import hep.dataforge.tables.TableFormatBuilder;
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

import static hep.dataforge.tables.XYAdapter.*;
import static inr.numass.data.api.NumassPoint.HV_KEY;

/**
 * An analyzer which uses time information from events
 * Created by darksnake on 11.07.2017.
 */
public class TimeAnalyzer extends AbstractAnalyzer {
    public static String T0_KEY = "t0";

    public static String[] NAME_LIST = {LENGTH_KEY, COUNT_KEY, COUNT_RATE_KEY, COUNT_RATE_ERROR_KEY, WINDOW_KEY, TIME_KEY, T0_KEY};
    public static String[] NAME_LIST_WITH_HV = {HV_KEY, LENGTH_KEY, COUNT_KEY, COUNT_RATE_KEY, COUNT_RATE_ERROR_KEY, WINDOW_KEY, TIME_KEY, T0_KEY};

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
                .filter(pair -> pair.getValue() >= t0)
                .forEach(pair -> {
                    totalN.incrementAndGet();
                    //TODO add progress listener here
                    totalT.addAndGet(pair.getValue());
                });

        double countRate = 1e6 * totalN.get() / (totalT.get() / 1000 - t0 * totalN.get() / 1000);//1e9 / (totalT.get() / totalN.get() - t0);
        double countRateError = countRate / Math.sqrt(totalN.get());
        double length = totalT.get() / 1e9;
        long count = (long) (length * countRate);


        if (block instanceof NumassPoint) {
            return ValueMap.of(NAME_LIST_WITH_HV,
                    ((NumassPoint) block).getVoltage(),
                    length,
                    count,
                    countRate,
                    countRateError,
                    new Integer[]{loChannel, upChannel},
                    block.getStartTime(),
                    (double)t0 / 1000d
            );
        } else {
            return ValueMap.of(NAME_LIST,
                    length,
                    count,
                    countRate,
                    countRateError,
                    new Integer[]{loChannel, upChannel},
                    block.getStartTime(),
                    (double)t0 / 1000d
            );
        }
    }

    private long getT0(NumassBlock block, Meta config) {
        return config.getValue("t0", 0).longValue();
    }

    /**
     * The chain of event times in nanos
     *
     * @param block
     * @param config
     * @return
     */
    public Stream<Pair<NumassEvent, Long>> getEventsWithDelay(NumassBlock block, Meta config) {
        AtomicReference<NumassEvent> lastEvent = new AtomicReference<>(null);

        Stream<NumassEvent> eventStream = super.getEvents(block, config);//using super implementation

        return eventStream.map(event -> {
            long res = lastEvent.get() == null ? -1L : event.getTimeOffset() - lastEvent.get().getTimeOffset();

            if (res < 0) {
                res = 0L;
            }

            lastEvent.set(event);
            return new Pair<>(event, res);
        });
    }

    /**
     * The filtered stream of events
     *
     * @param block
     * @param config
     * @return
     */
    @Override
    public Stream<NumassEvent> getEvents(NumassBlock block, Meta config) {
        long t0 = getT0(block, config);
        return getEventsWithDelay(block, config).filter(pair -> pair.getValue() >= t0).map(Pair::getKey);
    }

    @Override
    protected TableFormat getTableFormat(Meta config) {
        return new TableFormatBuilder()
                .addNumber(HV_KEY, X_VALUE_KEY)
                .addNumber(LENGTH_KEY)
                .addNumber(COUNT_KEY)
                .addNumber(COUNT_RATE_KEY, Y_VALUE_KEY)
                .addNumber(COUNT_RATE_ERROR_KEY, Y_ERROR_KEY)
                .addColumn(WINDOW_KEY)
                .addTime()
                .addNumber(T0_KEY)
                .build();
    }
}
