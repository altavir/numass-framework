package hep.dataforge.maths.histogram;

import hep.dataforge.maths.GridCalculator;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.DoubleStream;

/**
 * A univariate histogram with fast bin lookup.
 * Created by darksnake on 02.07.2017.
 */
public class UnivariateHistogram extends Histogram {
    public static UnivariateHistogram buildUniform(double start, double stop, double step) {
        return new UnivariateHistogram(GridCalculator.getUniformUnivariateGrid(start, stop, step));
    }

    private final double[] borders;
    private TreeMap<Double, Bin> binMap = new TreeMap<>();

    public UnivariateHistogram(double[] borders) {
        this.borders = borders;
        Arrays.sort(borders);
    }

    private Double getValue(Double... point) {
        if (point.length != 1) {
            throw new DimensionMismatchException(point.length, 1);
        } else {
            return point[0];
        }
    }

    @Override
    public Bin createBin(Double... point) {
        Double value = getValue(point);
        int index = Arrays.binarySearch(borders, value);
        if (index >= 0) {
            if (index == borders.length - 1) {
                return new SquareBin(borders[index], Double.POSITIVE_INFINITY);
            } else {
                return new SquareBin(borders[index], borders[index + 1]);
            }
        } else if (index == -1) {
            return new SquareBin(Double.NEGATIVE_INFINITY, borders[0]);
        } else if (index == -borders.length - 1) {
            return new SquareBin(borders[borders.length - 1], Double.POSITIVE_INFINITY);
        } else {
            return new SquareBin(borders[-index - 2], borders[-index - 1]);
        }
    }

    @Override
    public Optional<Bin> findBin(Double... point) {
        Double value = getValue(point);
        Map.Entry<Double, Bin> entry = binMap.floorEntry(value);
        if (entry != null && entry.getValue().contains(point)) {
            return Optional.of(entry.getValue());
        } else {
            return Optional.empty();
        }

    }

    @Override
    protected synchronized Bin addBin(Bin bin) {
        //The call should be thread safe. New bin is added only if it is absent
        return binMap.computeIfAbsent(bin.getLowerBound(0), (id) -> bin);
    }

    @Override
    public int getDimension() {
        return 1;
    }

    @NotNull
    @Override
    public Iterator<Bin> iterator() {
        return binMap.values().iterator();
    }

    public UnivariateHistogram fill(DoubleStream stream) {
        stream.forEach(this::put);
        return this;
    }

//    public UnivariateHistogram fill(LongStream stream) {
//        stream.mapToDouble(it -> it).forEach(this::put);
//        return this;
//    }
}
