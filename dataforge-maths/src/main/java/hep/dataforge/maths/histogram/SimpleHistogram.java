package hep.dataforge.maths.histogram;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A simple histogram with square bins and slow lookup
 * Created by darksnake on 29-Jun-17.
 */
public class SimpleHistogram extends Histogram {

    private final UniformBinFactory binFactory;
    private final Map<Long, Bin> binMap = new HashMap<>();

    public SimpleHistogram(Double[] binStart, Double[] binStep) {
        this.binFactory = new UniformBinFactory(binStart, binStep);
    }

    public SimpleHistogram(Double binStart, Double binStep) {
        this.binFactory = new UniformBinFactory(new Double[]{binStart}, new Double[]{binStep});
    }

    @Override
    public SquareBin createBin(Double... point) {
        return binFactory.createBin(point);
    }

    @Override
    public synchronized Optional<Bin> findBin(Double... point) {
        //Simple slow lookup mechanism
        return binMap.values().stream().filter(bin -> bin.contains(point)).findFirst();
    }

    @Override
    protected synchronized Bin addBin(Bin bin) {
        //The call should be thread safe. New bin is added only if it is absent
        return binMap.computeIfAbsent(bin.getBinID(), (id) -> bin);
    }
//
//    @Override
//    public Bin getBinById(long id) {
//        return binMap.get(id);
//    }

    @NotNull
    @Override
    public Iterator<Bin> iterator() {
        return binMap.values().iterator();
    }

    @Override
    public Stream<Bin> binStream() {
        return binMap.values().stream();
    }


    @Override
    public int getDimension() {
        return this.binFactory.getDimension();
    }
}
