package inr.numass.data.events;

import hep.dataforge.meta.Metoid;

import java.util.stream.Stream;

/**
 * Created by darksnake on 06-Jul-17.
 */
public interface NumassPoint extends Metoid {

    

    Stream<NumassBlock> getBlocks();
}
