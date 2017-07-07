package inr.numass.data.analyzers;

import hep.dataforge.meta.Meta;
import hep.dataforge.tables.ValueMap;
import hep.dataforge.values.Values;
import inr.numass.data.api.NumassAnalyzer;
import inr.numass.data.api.NumassBlock;
import inr.numass.data.api.NumassEvent;
import inr.numass.data.api.SignalProcessor;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * A simple event counter
 * Created by darksnake on 07.07.2017.
 */
public class SimpleAnalyzer implements NumassAnalyzer {
    public static String[] NAME_LIST = {"length", "count", COUNT_RATE_KEY, COUNT_RATE_ERROR_KEY, "window", "timestamp"};

    @Nullable
    private final SignalProcessor processor;

    public SimpleAnalyzer(@Nullable SignalProcessor processor) {
        this.processor = processor;
    }

    /**
     * Return unsorted stream of events including events from frames
     *
     * @param block
     * @return
     */
    private Stream<NumassEvent> getEventStream(NumassBlock block) {
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
