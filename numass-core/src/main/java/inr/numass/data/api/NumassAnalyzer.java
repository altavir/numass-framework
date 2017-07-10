package inr.numass.data.api;

import hep.dataforge.meta.Meta;
import hep.dataforge.tables.Table;
import hep.dataforge.values.Values;

/**
 * A general raw data analysis utility. Could have different implementations
 * Created by darksnake on 06-Jul-17.
 */
public interface NumassAnalyzer {
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
     * Analyze the whole set. And return results as a table
     * @param set
     * @param config
     * @return
     */
    Table analyze(NumassSet set, Meta config);

    /**
     * Generate energy spectrum for the given block
     * @param block
     * @param config
     * @return
     */
    Table getSpectrum(NumassBlock block, Meta config);


}
