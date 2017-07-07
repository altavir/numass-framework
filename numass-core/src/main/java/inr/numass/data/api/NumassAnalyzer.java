package inr.numass.data.api;

import hep.dataforge.meta.Meta;
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


}
