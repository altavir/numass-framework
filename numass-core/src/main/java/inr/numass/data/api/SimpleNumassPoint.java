package inr.numass.data.api;

import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.utils.MetaHolder;

import java.util.List;
import java.util.stream.Stream;

/**
 * A simple static implementation of NumassPoint
 * Created by darksnake on 08.07.2017.
 */
public class SimpleNumassPoint extends MetaHolder implements NumassPoint {
    private final List<NumassBlock> blocks;

    public SimpleNumassPoint(double voltage, List<NumassBlock> blocks) {
        this.blocks = blocks;
        super.setMeta(new MetaBuilder("point").setValue(HV_KEY, voltage));
    }

    public SimpleNumassPoint(Meta meta, List<NumassBlock> blocks) {
        super(meta);
        this.blocks = blocks;
    }

    @Override
    public Stream<NumassBlock> getBlocks() {
        return blocks.stream();
    }
}
