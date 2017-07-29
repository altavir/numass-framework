package inr.numass.data.api;

import hep.dataforge.meta.Meta;
import hep.dataforge.tables.*;
import hep.dataforge.values.Value;
import hep.dataforge.values.Values;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
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
            return channel > loChannel && channel < upChannel;
        }).mapToLong(it -> it.getValue(COUNT_KEY).numberValue().longValue()).sum();
    }

    /**
     * Apply window and binning to a spectrum
     *
     * @param binSize
     * @return
     */
    static Table spectrumWithBinning(Table spectrum, int binSize) {
        TableFormat format = new TableFormatBuilder()
                .addNumber(CHANNEL_KEY, X_VALUE_KEY)
                .addNumber(COUNT_KEY, Y_VALUE_KEY)
                .addNumber(COUNT_RATE_KEY)
                .addNumber("binSize");
        ListTable.Builder builder = new ListTable.Builder(format);
        int loChannel = spectrum.getColumn(CHANNEL_KEY).stream().mapToInt(Value::intValue).min().orElse(0);
        int upChannel = spectrum.getColumn(CHANNEL_KEY).stream().mapToInt(Value::intValue).max().orElse(1);

        for (int chan = loChannel; chan < upChannel - binSize; chan += binSize) {
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
            int bin = Math.min(binSize, upChannel - chan);
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

        //optimized for fastest computation
        //TODO requires additional performance optimization
        AtomicLong[] spectrum = new AtomicLong[MAX_CHANNEL];
        getEventStream(block, config).forEach(event -> {
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
                            return new ValueMap(
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
