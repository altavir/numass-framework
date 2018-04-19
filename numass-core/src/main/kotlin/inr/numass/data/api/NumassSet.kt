/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.data.api

import hep.dataforge.Named
import hep.dataforge.kodex.toList
import hep.dataforge.meta.Metoid
import hep.dataforge.providers.Provider
import hep.dataforge.providers.Provides
import hep.dataforge.providers.ProvidesNames
import hep.dataforge.tables.Table
import java.time.Instant
import java.util.*
import java.util.stream.Stream

/**
 * A single set of numass points previously called file.
 *
 * @author [Alexander Nozik](mailto:altavir@gmail.com)
 */
interface NumassSet : Named, Metoid, Iterable<NumassPoint>, Provider {

    val points: Stream<out NumassPoint>

    /**
     * Get the first point if it exists. Throw runtime exception otherwise.
     *
     * @return
     */
    val firstPoint: NumassPoint
        get() = points.findFirst().orElseThrow { RuntimeException("The set is empty") }

    /**
     * Get the starting time from meta or from first point
     *
     * @return
     */
    val startTime: Instant
        get() = meta.optValue(NumassPoint.START_TIME_KEY).map<Instant>{ it.getTime() }.orElseGet { firstPoint.startTime }

    val hvData: Optional<Table>
        get() = Optional.empty()

    //    default String getDescription() {
    //        return getMeta().getString(DESCRIPTION_KEY, "");
    //    }

    override fun iterator(): Iterator<NumassPoint> {
        return points.iterator()
    }

    /**
     * Find first point with given voltage
     *
     * @param voltage
     * @return
     */
    fun optPoint(voltage: Double): Optional<out NumassPoint> {
        return points.filter { it -> it.voltage == voltage }.findFirst()
    }

    /**
     * List all points with given voltage
     *
     * @param voltage
     * @return
     */
    fun getPoints(voltage: Double): List<NumassPoint> {
        return points.filter { it -> it.voltage == voltage }.toList()
    }

    @Provides(NUMASS_POINT_PROVIDER_KEY)
    fun optPoint(voltage: String): Optional<out NumassPoint> {
        return optPoint(java.lang.Double.parseDouble(voltage))
    }

    override fun defaultTarget(): String {
        return NUMASS_POINT_PROVIDER_KEY
    }

    @ProvidesNames(NUMASS_POINT_PROVIDER_KEY)
    fun listPoints(): Stream<String> {
        return points.map { it -> java.lang.Double.toString(it.voltage) }
    }

    companion object {
        const val DESCRIPTION_KEY = "info"
        const val NUMASS_POINT_PROVIDER_KEY = "point"
    }
}
