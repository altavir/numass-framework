package hep.dataforge.maths.histogram;

import hep.dataforge.names.NameList;
import hep.dataforge.names.NamesUtils;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.Table;
import hep.dataforge.tables.TableFormat;
import hep.dataforge.tables.TableFormatBuilder;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static hep.dataforge.tables.Adapters.X_VALUE_KEY;
import static hep.dataforge.tables.Adapters.Y_VALUE_KEY;

/**
 * A thread safe histogram
 * Created by darksnake on 29-Jun-17.
 */
public abstract class Histogram implements BinFactory, Iterable<Bin> {

    /**
     * Lookup a bin containing specific point if it is present
     *
     * @param point
     * @return
     */
    public abstract Optional<Bin> findBin(Double... point);

    /**
     * Add a bin to storage
     *
     * @param bin
     * @return
     */
    protected abstract Bin addBin(Bin bin);

    /**
     * Find or create a bin containing given point and return number of counts in bin after addition
     *
     * @param point
     * @return
     */
    public long put(Double... point) {
        Bin bin = findBin(point).orElseGet(() -> addBin(createBin(point)));
        //PENDING add ability to do some statistical analysis on flight?
        return bin.inc();
    }

    public Histogram fill(Stream<Double[]> stream) {
        stream.parallel().forEach(this::put);
        return this;
    }

    public Histogram fill(Iterable<Double[]> iter) {
        iter.forEach(this::put);
        return this;
    }

//    public abstract Bin getBinById(long id);

    public Stream<Bin> binStream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Construct a format for table using given names as axis names. The number of input names should equal to the
     * dimension of this histogram or exceed it by one. In later case the last name is count axis name.
     *
     * @return
     */
    protected TableFormat getFormat() {
        TableFormatBuilder builder = new TableFormatBuilder();
        for (String axisName : getNames()) {
            builder.addNumber(axisName, X_VALUE_KEY);
//            builder.addNumber(axisName + ".binEnd");
        }
        builder.addNumber("count", Y_VALUE_KEY);
        builder.addColumn("id");
        return builder.build();
    }


    /**
     * @return
     */
    public Table asTable() {
        return new ListTable(getFormat(), binStream().map(Bin::describe).collect(Collectors.toList()));
    }

    public abstract int getDimension();

    /**
     * Get axis names excluding count axis
     *
     * @return
     */
    public NameList getNames() {
        return NamesUtils.generateNames(getDimension());
    }
}

