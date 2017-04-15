/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.data;

import hep.dataforge.meta.Annotated;
import hep.dataforge.meta.Meta;
import hep.dataforge.names.Named;
import hep.dataforge.tables.Table;

import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:altavir@gmail.com">Alexander Nozik</a>
 */
public interface NumassData extends Named, Annotated, Iterable<NumassPoint> {

    String getDescription();

    @Override
    Meta meta();

    Stream<NumassPoint> stream();

    @Override
    default Iterator<NumassPoint> iterator() {
        return stream().iterator();
    }

    default List<NumassPoint> getNMPoints() {
        return stream().collect(Collectors.toList());
    }

    boolean isEmpty();

    Instant startTime();

    default Supplier<Table> getHVData() {
        return () -> null;
    }

    /**
     * Find first point with given Uset
     *
     * @param U
     * @return
     */
    default NumassPoint getByVoltage(double U) {
        for (NumassPoint point : this) {
            if (point.getVoltage() == U) {
                return point;
            }
        }
        return null;
    }


}
