package inr.numass.data.analyzers;

import hep.dataforge.description.ValueDef;
import hep.dataforge.meta.Meta;
import hep.dataforge.tables.TableFormat;
import hep.dataforge.tables.TableFormatBuilder;
import hep.dataforge.tables.ValueMap;
import hep.dataforge.values.Value;
import hep.dataforge.values.ValueType;
import hep.dataforge.values.Values;
import inr.numass.data.api.NumassBlock;
import inr.numass.data.api.NumassEvent;
import inr.numass.data.api.NumassPoint;
import inr.numass.data.api.SignalProcessor;
import javafx.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static hep.dataforge.tables.Adapters.*;
import static inr.numass.data.api.NumassPoint.HV_KEY;

/**
 * An analyzer which uses time information from events
 * Created by darksnake on 11.07.2017.
 */
public class TimeAnalyzer extends AbstractAnalyzer {
    public static String T0_KEY = "t0";

    public static final String[] NAME_LIST = {LENGTH_KEY, COUNT_KEY, COUNT_RATE_KEY, COUNT_RATE_ERROR_KEY, WINDOW_KEY, TIME_KEY, T0_KEY};
//    public static String[] NAME_LIST_WITH_HV = {HV_KEY, LENGTH_KEY, COUNT_KEY, COUNT_RATE_KEY, COUNT_RATE_ERROR_KEY, WINDOW_KEY, TIME_KEY, T0_KEY};

    public TimeAnalyzer(@Nullable SignalProcessor processor) {
        super(processor);
    }

    public TimeAnalyzer() {
    }

    @Override
    public Values analyze(NumassBlock block, Meta config) {
        //In case points inside points
        if (block instanceof NumassPoint) {
            return analyzePoint((NumassPoint) block, config);
        }


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

        return ValueMap.of(NAME_LIST,
                length,
                count,
                countRate,
                countRateError,
                new Integer[]{loChannel, upChannel},
                block.getStartTime(),
                (double) t0 / 1000d
        );
    }

    @Override
    public Values analyzePoint(NumassPoint point, Meta config) {
        //Average count rates, do not sum events
        Values res = point.getBlocks()
                .map(it -> analyze(it, config))
                .reduce(null, this::combineBlockResults);

        Map<String, Value> map = new HashMap<>(res.asMap());
        map.put(HV_KEY, Value.of(point.getVoltage()));
        return new ValueMap(map);
    }

    /**
     * Combine two blocks from the same point into one
     *
     * @param v1
     * @param v2
     * @return
     */
    private Values combineBlockResults(Values v1, Values v2) {
        if (v1 == null) {
            return v2;
        }
        if (v2 == null) {
            return v1;
        }


        double t1 = v1.getDouble(LENGTH_KEY);
        double t2 = v2.getDouble(LENGTH_KEY);
        double cr1 = v1.getDouble(COUNT_RATE_KEY);
        double cr2 = v2.getDouble(COUNT_RATE_KEY);
        double err1 = v1.getDouble(COUNT_RATE_ERROR_KEY);
        double err2 = v2.getDouble(COUNT_RATE_ERROR_KEY);

        double countRate = (t1 * cr1 + t2 * cr2) / (t1 + t2);

        double countRateErr = Math.sqrt(Math.pow(t1 * err1 / (t1 + t2), 2) + Math.pow(t2 * err2 / (t1 + t2), 2));


        return ValueMap.of(NAME_LIST,
                v1.getDouble(LENGTH_KEY) + v2.getDouble(LENGTH_KEY),
                v1.getInt(COUNT_KEY) + v2.getInt(COUNT_KEY),
                countRate,
                countRateErr,
                v1.getValue(WINDOW_KEY),
                v1.getValue(TIME_KEY),
                v1.getDouble(T0_KEY)
        );
    }

    @ValueDef(name = "t0", type = ValueType.NUMBER, info = "Constant t0 cut")
    @ValueDef(name = "t0.crFraction", type = ValueType.NUMBER, info = "The relative fraction of events that should be removed by time cut")
    @ValueDef(name = "t0.min", type = ValueType.NUMBER, def = "0", info = "Minimal t0")
    private int getT0(NumassBlock block, Meta meta) {
        if (meta.hasValue("t0")) {
            return meta.getInt("t0");
        } else if (meta.hasMeta("t0")) {
            double fraction = meta.getDouble("t0.crFraction");
            double cr = estimateCountRate(block);
            if (cr < meta.getDouble("t0.minCR", 0)) {
                return 0;
            } else {
                return (int) Math.max(-1e9 / cr * Math.log(1d - fraction), meta.getDouble("t0.min", 0));
            }
        } else {
            return 0;
        }

    }

    private double estimateCountRate(NumassBlock block) {
        return (double) block.getEvents().count() / block.getLength().toMillis() * 1000;
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
            long res = lastEvent.get() == null ? 0L : event.getTimeOffset() - lastEvent.get().getTimeOffset();

            if (res < 0) {
                res = 0L;
            }

            lastEvent.set(event);
            //TODO remove autoboxing somehow
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
