package inr.numass.data.api;

import hep.dataforge.meta.Meta;
import hep.dataforge.tables.*;
import hep.dataforge.values.Values;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static hep.dataforge.tables.XYAdapter.*;

/**
 * A general raw data analysis utility. Could have different implementations
 * Created by darksnake on 06-Jul-17.
 */
public interface NumassAnalyzer {

    /**
     * Calculate number of counts in the given channel
     * @param spectrum
     * @param loChannel
     * @param upChannel
     * @return
     */
    static long countInWindow(Table spectrum, short loChannel, short upChannel) {
        return spectrum.getRows().filter(row -> {
            int channel = row.getInt(CHANNEL_KEY);
            return channel > loChannel && channel < upChannel;
        }).mapToLong(it -> it.getValue(COUNT_KEY).numberValue().longValue()).sum();
    }

    /**
     * Apply window and binning to a spectrum
     *
     * @param lo
     * @param up
     * @param binSize
     * @return
     */
    static Table spectrumWithBinning(Table spectrum, int lo, int up, int binSize) {
        TableFormat format = new TableFormatBuilder()
                .addNumber(CHANNEL_KEY, X_VALUE_KEY)
                .addNumber(COUNT_KEY, Y_VALUE_KEY)
                .addNumber(COUNT_RATE_KEY)
                .addNumber("binSize");
        ListTable.Builder builder = new ListTable.Builder(format);
        for (int chan = lo; chan < up - binSize; chan += binSize) {
            AtomicLong count = new AtomicLong(0);
            AtomicReference<Double> countRate = new AtomicReference<>(0d);

            int binLo = chan;
            int binUp = chan + binSize;

            spectrum.getRows().filter(row -> {
                int c = row.getInt(CHANNEL_KEY);
                return c >= binLo && c <= binUp;
            }).forEach(row -> {
                count.addAndGet(row.getValue(COUNT_KEY).numberValue().longValue());
                countRate.accumulateAndGet(row.getDouble(COUNT_RATE_KEY), (d1, d2) -> d1 + d2);
            });
            int bin = Math.min(binSize, up - chan);
            builder.row((double) chan + (double) bin / 2d, count.get(), countRate.get(), bin);
        }
        return builder.build();
    }

    String CHANNEL_KEY = "channel";
    String COUNT_KEY = "count";
    String LENGTH_KEY = "length";
    String COUNT_RATE_KEY = "cr";
    String COUNT_RATE_ERROR_KEY = "cr.err";

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
    Stream<NumassEvent> getEventStream(NumassBlock block, Meta config);

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
        NavigableMap<Short, AtomicLong> map = new TreeMap<>();
        getEventStream(block, config).forEach(event -> {
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
                                        (double) entry.getValue().get() / block.getLength().toMillis() * 1000d,
                                        Math.sqrt(entry.getValue().get()) / block.getLength().toMillis() * 1000d
                                )
                        )
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
