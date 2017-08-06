package inr.numass.data.api;

import hep.dataforge.meta.Meta;
import hep.dataforge.tables.*;
import hep.dataforge.values.Values;

import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static hep.dataforge.tables.XYAdapter.*;

/**
 * A general raw data analysis utility. Could have different implementations
 * Created by darksnake on 06-Jul-17.
 */
public interface NumassAnalyzer {
    short MAX_CHANNEL = 10000;

    /**
     * Calculate number of counts in the given channel
     *
     * @param spectrum
     * @param loChannel
     * @param upChannel
     * @return
     */
    static long countInWindow(Table spectrum, short loChannel, short upChannel) {
        return spectrum.getRows().filter(row -> {
            int channel = row.getInt(CHANNEL_KEY);
            return channel >= loChannel && channel < upChannel;
        }).mapToLong(it -> it.getValue(COUNT_KEY).numberValue().longValue()).sum();
    }



    String CHANNEL_KEY = "channel";
    String COUNT_KEY = "count";
    String LENGTH_KEY = "length";
    String COUNT_RATE_KEY = "cr";
    String COUNT_RATE_ERROR_KEY = "crErr";

    /**
     * Perform analysis on block. The values for count rate, its error and point length in nanos must
     * exist, but occasionally additional values could also be presented.
     *
     * @param block
     * @return
     */
    Values analyze(NumassBlock block, Meta config);

    /**
     * Return unsorted stream of events including events from frames
     *
     * @param block
     * @return
     */
    Stream<NumassEvent> getEvents(NumassBlock block, Meta config);

    /**
     * Analyze the whole set. And return results as a table
     *
     * @param set
     * @param config
     * @return
     */
    Table analyze(NumassSet set, Meta config);

    /**
     * Calculate the energy spectrum for a given block. The s
     *
     * @param block
     * @param config
     * @return
     */
    default Table getSpectrum(NumassBlock block, Meta config) {
        TableFormat format = new TableFormatBuilder()
                .addNumber(CHANNEL_KEY, X_VALUE_KEY)
                .addNumber(COUNT_KEY)
                .addNumber(COUNT_RATE_KEY, Y_VALUE_KEY)
                .addNumber(COUNT_RATE_ERROR_KEY, Y_ERROR_KEY)
                .updateMeta(metaBuilder -> metaBuilder.setNode("config", config))
                .build();

        //optimized for fastest computation
        //TODO requires additional performance optimization
        AtomicLong[] spectrum = new AtomicLong[MAX_CHANNEL];
        getEvents(block, config).forEach(event -> {
            if (spectrum[event.getChanel()] == null) {
                spectrum[event.getChanel()] = new AtomicLong(1);
            } else {
                spectrum[event.getChanel()].incrementAndGet();
            }
        });

        double seconds = (double) block.getLength().toMillis() / 1000d;
        return new ListTable.Builder(format)
                .rows(IntStream.range(0, MAX_CHANNEL)
                        .filter(i -> spectrum[i] != null)
                        .mapToObj(i -> {
                            long value = spectrum[i].get();
                            return ValueMap.of(
                                    format.namesAsArray(),
                                    i,
                                    value,
                                    (double) value / seconds,
                                    Math.sqrt(value) / seconds
                            );
                        })
                ).build();
    }

    /**
     * Get the approximate number of events in block. Not all analyzers support precise event counting
     *
     * @param block
     * @param config
     * @return
     */
    default long getCount(NumassBlock block, Meta config) {
        return analyze(block, config).getValue(COUNT_KEY).numberValue().longValue();
    }

    /**
     * Get approximate effective point length in nanos. It is not necessary corresponds to real point length.
     *
     * @param block
     * @param config
     * @return
     */
    default long getLength(NumassBlock block, Meta config) {
        return analyze(block, config).getValue(LENGTH_KEY).numberValue().longValue();
    }
}
