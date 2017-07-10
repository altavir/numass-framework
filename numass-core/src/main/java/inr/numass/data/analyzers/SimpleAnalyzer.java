package inr.numass.data.analyzers;

import hep.dataforge.meta.Meta;
import hep.dataforge.tables.*;
import hep.dataforge.values.Values;
import inr.numass.data.api.*;
import org.jetbrains.annotations.Nullable;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static hep.dataforge.tables.XYAdapter.*;
import static inr.numass.data.api.NumassPoint.HV_KEY;

/**
 * A simple event counter
 * Created by darksnake on 07.07.2017.
 */
public class SimpleAnalyzer implements NumassAnalyzer {
    public static String[] NAME_LIST = {"length", "count", COUNT_RATE_KEY, COUNT_RATE_ERROR_KEY, "window", "timestamp"};
    public static String[] NAME_LIST_WITH_HV = {HV_KEY, "length", "count", COUNT_RATE_KEY, COUNT_RATE_ERROR_KEY, "window", "timestamp"};

    @Nullable
    private final SignalProcessor processor;

    public SimpleAnalyzer(@Nullable SignalProcessor processor) {
        this.processor = processor;
    }

    public SimpleAnalyzer() {
        this.processor = null;
    }

    /**
     * Return unsorted stream of events including events from frames
     *
     * @param block
     * @return
     */
    protected Stream<NumassEvent> getEventStream(NumassBlock block) {
        if (processor == null && block.getFrames().count() > 0) {
            throw new IllegalArgumentException("Signal processor needed to analyze frames");
        } else {
            return Stream.concat(block.getEvents(), block.getFrames().flatMap(processor::analyze));
        }
    }

    @Override
    public Values analyze(NumassBlock block, Meta config) {
        int loChannel = config.getInt("energy.lo", 0);
        int upChannel = config.getInt("energy.up", Integer.MAX_VALUE);
        long count = getEventStream(block)
                .filter(it -> it.getChanel() >= loChannel && it.getChanel() <= upChannel)
                .count();
        double countRate = (double) count / block.getLength().toMillis() * 1000;
        double countRateError = Math.sqrt((double) count) / block.getLength().toMillis() * 1000;

        if (block instanceof NumassPoint) {
            return new ValueMap(NAME_LIST_WITH_HV,
                    new Object[]{
                            ((NumassPoint) block).getVoltage(),
                            block.getLength().toNanos(),
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
                            block.getLength().toNanos(),
                            count,
                            countRate,
                            countRateError,
                            new int[]{loChannel, upChannel},
                            block.getStartTime()
                    }
            );
        }
    }


    @Override
    public Table analyze(NumassSet set, Meta config) {
        TableFormat format = new TableFormatBuilder()
                .addNumber(HV_KEY, X_VALUE_KEY)
                .addNumber("length")
                .addNumber("count")
                .addNumber(COUNT_RATE_KEY, Y_VALUE_KEY)
                .addNumber(COUNT_RATE_ERROR_KEY, Y_ERROR_KEY)
                .addColumn("window")
                .addTime()
                .build();

        return new ListTable.Builder(format)
                .rows(set.getPoints().map(point -> analyze(point, config)))
                .build();
    }

    @Override
    public Table getSpectrum(NumassBlock block, Meta config) {
        TableFormat format = new TableFormatBuilder()
                .addNumber("channel", X_VALUE_KEY)
                .addNumber("count")
                .addNumber(COUNT_RATE_KEY, Y_VALUE_KEY)
                .addNumber(COUNT_RATE_ERROR_KEY, Y_ERROR_KEY)
                .updateMeta(metaBuilder -> metaBuilder.setNode("config", config))
                .build();
        NavigableMap<Short, AtomicLong> map = new TreeMap<>();
        getEventStream(block).forEach(event -> {
            if (map.containsKey(event.getChanel())) {
                map.get(event.getChanel()).incrementAndGet();
            } else {
                map.put(event.getChanel(), new AtomicLong(1));
            }
        });
        return new ListTable.Builder(format)
                .rows(map.entrySet().stream()
                        .map(entry ->
                                new ValueMap(format.namesAsArray(),
                                        entry.getKey(),
                                        entry.getValue(),
                                        entry.getValue().get() / block.getLength().toMillis() * 1000,
                                        Math.sqrt(entry.getValue().get()) / block.getLength().toMillis() * 1000
                                )
                        )
                ).build();
    }
}
