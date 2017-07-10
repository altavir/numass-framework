/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.data.api;

import hep.dataforge.data.Data;
import hep.dataforge.meta.Metoid;
import hep.dataforge.names.Named;
import hep.dataforge.providers.Provider;
import hep.dataforge.providers.Provides;
import hep.dataforge.providers.ProvidesNames;
import hep.dataforge.tables.Table;
import hep.dataforge.values.Value;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A single set of numass points previously called file.
 *
 * @author <a href="mailto:altavir@gmail.com">Alexander Nozik</a>
 */
public interface NumassSet extends Named, Metoid, Iterable<NumassPoint>, Provider {
    String DESCRIPTION_KEY = "info";
    String NUMASS_POINT_PROVIDER_KEY = "point";

    Stream<NumassPoint> getPoints();

    default String getDescription() {
        return meta().getString(DESCRIPTION_KEY, "");
    }

    @NotNull
    @Override
    default Iterator<NumassPoint> iterator() {
        return getPoints().iterator();
    }

    /**
     * Get the first point if it exists. Throw runtime exception otherwise.
     *
     * @return
     */
    default NumassPoint getFirstPoint() {
        return getPoints().findFirst().orElseThrow(() -> new RuntimeException("The set is empty"));
    }

    /**
     * Get the starting time from meta or from first point
     *
     * @return
     */
    default Instant getStartTime() {
        return meta().optValue(NumassPoint.START_TIME_KEY).map(Value::timeValue).orElseGet(() -> getFirstPoint().getStartTime());
    }

    /**
     * Find first point with given voltage
     *
     * @param voltage
     * @return
     */
    default Optional<NumassPoint> optPoint(double voltage) {
        return getPoints().filter(it -> it.getVoltage() == voltage).findFirst();
    }

    /**
     * List all points with given voltage
     * @param voltage
     * @return
     */
    default List<NumassPoint> listPoints(double voltage){
        return getPoints().filter(it -> it.getVoltage() == voltage).collect(Collectors.toList());
    }

    @Provides(NUMASS_POINT_PROVIDER_KEY)
    default Optional<NumassPoint> optPoint(String voltage) {
        return optPoint(Double.parseDouble(voltage));
    }

    @Override
    default String defaultTarget() {
        return NUMASS_POINT_PROVIDER_KEY;
    }

    @ProvidesNames(NUMASS_POINT_PROVIDER_KEY)
    default Stream<String> listPoints() {
        return getPoints().map(it -> Double.toString(it.getVoltage()));
    }

    default Data<Table> getHvData() {
        return Data.buildStatic(null);
    }
}
