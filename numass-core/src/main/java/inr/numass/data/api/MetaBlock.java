package inr.numass.data.api;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

/**
 * A block constructed from a set of other blocks. Internal blocks are not necessary subsequent. Blocks are automatically sorted.
 * Created by darksnake on 16.07.2017.
 */
public class MetaBlock implements NumassBlock {
    private SortedSet<NumassBlock> blocks = new TreeSet<>(Comparator.comparing(NumassBlock::getStartTime));

    public MetaBlock(NumassBlock... blocks) {
        this.blocks.addAll(Arrays.asList(blocks));
    }

    public MetaBlock(Collection<NumassBlock> blocks) {
        this.blocks.addAll(blocks);
    }

    @Override
    public Instant getStartTime() {
        return blocks.first().getStartTime();
    }

    @Override
    public Duration getLength() {
        return Duration.ofNanos(blocks.stream().mapToLong(block -> block.getLength().toNanos()).sum());
    }

    @Override
    public Stream<NumassEvent> getEvents() {
        return blocks.stream()
                .sorted(Comparator.comparing(NumassBlock::getStartTime))
                .flatMap(NumassBlock::getEvents);
    }

    @Override
    public Stream<NumassFrame> getFrames() {
        return blocks.stream()
                .sorted(Comparator.comparing(NumassBlock::getStartTime))
                .flatMap(NumassBlock::getFrames);
    }
}
