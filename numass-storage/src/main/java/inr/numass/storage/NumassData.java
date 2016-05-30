/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.storage;

import hep.dataforge.meta.Annotated;
import hep.dataforge.meta.Meta;
import hep.dataforge.names.Named;
import hep.dataforge.tables.Table;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

/**
 *
 * @author Alexander Nozik <altavir@gmail.com>
 */
public interface NumassData extends Named, Annotated {

    String getDescription();

    @Override
    Meta meta();

    List<NMPoint> getNMPoints();

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
    default NMPoint getByUset(double U) {
        for (NMPoint point : getNMPoints()) {
            if (point.getUset() == U) {
                return point;
            }
        }
        return null;
    }

    /**
     * Find first point with given Uread
     *
     * @param U
     * @return
     */
    default NMPoint getByUread(double U) {
        for (NMPoint point : getNMPoints()) {
            if (point.getUread() == U) {
                return point;
            }
        }
        return null;
    }

}
