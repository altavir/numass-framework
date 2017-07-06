package inr.numass.data.events;

import hep.dataforge.meta.Meta;
import hep.dataforge.tables.Table;

/**
 * A general raw data analysis utility. Could have different implementations
 * Created by darksnake on 06-Jul-17.
 */
public interface NumassAnalyzer {
    /**
     * Caclulate the number of events in given window
     *
     * @param block
     * @param from
     * @param to
     * @return
     */
    int getCountInWindow(NumassBlock block, int from, int to);

    default int getMaxChannel() {
        return 4096;
    }

    default int getCountTotal(NumassBlock block) {
        return getCountInWindow(block, 0, getMaxChannel());
    }

    public Table getSpectrum(Meta config);
}
