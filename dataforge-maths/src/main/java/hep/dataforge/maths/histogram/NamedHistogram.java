package hep.dataforge.maths.histogram;

import hep.dataforge.names.NameList;
import hep.dataforge.names.NameSetContainer;
import hep.dataforge.values.Values;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Named wrapper for histogram
 * Created by darksnake on 30.06.2017.
 */
public class NamedHistogram extends Histogram implements NameSetContainer {
    private final Histogram histogram;
    private final NameList names;

    public NamedHistogram(Histogram histogram, NameList names) {
        this.histogram = histogram;
        this.names = names;
    }

    public long put(Values point) {
        return put(extract(point));
    }

    /**
     * Put all value sets
     *
     * @param iter
     */
    public void putAllPoints(Iterable<? extends Values> iter) {
        iter.forEach(this::put);
    }

    public void putAllPoints(Stream<? extends Values> stream) {
        stream.parallel().forEach(this::put);
    }

    /**
     * Extract numeric vector from the point
     *
     * @param set
     * @return
     */
    private Double[] extract(Values set) {
        return names.stream().map(set::getDouble).toArray(Double[]::new);
    }

    @Override
    public Bin createBin(Double... point) {
        return histogram.createBin(point);
    }

    @Override
    public Optional<Bin> findBin(Double... point) {
        return histogram.findBin(point);
    }

    @Override
    protected Bin addBin(Bin bin) {
        return histogram.addBin(bin);
    }

//    @Override
//    public Bin getBinById(long id) {
//        return histogram.getBinById(id);
//    }

    @NotNull
    @Override
    public Iterator<Bin> iterator() {
        return histogram.iterator();
    }

    @Override
    public NameList getNames() {
        return names;
    }

    @Override
    public int getDimension() {
        return histogram.getDimension();
    }
}
