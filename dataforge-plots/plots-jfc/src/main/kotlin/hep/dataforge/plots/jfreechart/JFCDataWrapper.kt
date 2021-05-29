/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.plots.jfreechart

import hep.dataforge.names.Name
import hep.dataforge.plots.Plot
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.ValuesAdapter
import hep.dataforge.values.Values
import hep.dataforge.values.nullableDouble
import org.jfree.data.xy.AbstractIntervalXYDataset

/**
 * Wrapper for plot. Multiple xs are not allowed
 *
 * @author Alexander Nozik
 */
internal class JFCDataWrapper(val index: Int, private var plot: Plot) : AbstractIntervalXYDataset() {

    private val adapter: ValuesAdapter
        get() = plot.adapter

    private inner class JFCValuesWrapper(val values: Values) {
        val x: Number? by lazy { Adapters.getXValue(adapter, values).nullableDouble }

        val y: Number? by lazy { Adapters.getYValue(adapter, values).nullableDouble }

        val startX: Number? by lazy { Adapters.getLowerBound(adapter, Adapters.X_AXIS, values) }
        val endX: Number? by lazy { Adapters.getUpperBound(adapter, Adapters.X_AXIS, values) }

        val startY: Number? by lazy { Adapters.getLowerBound(adapter, Adapters.Y_AXIS, values) }
        val endY: Number? by lazy { Adapters.getUpperBound(adapter, Adapters.Y_AXIS, values) }

    }

    private var cache: List<JFCValuesWrapper>? = null


    fun setPlot(plot: Plot) {
        synchronized(this) {
            this.plot = plot
            cache = null
        }
    }

    private val data: List<JFCValuesWrapper>
        get() {
            synchronized(this) {

                if (cache == null) {
                    cache = plot.data.map { JFCValuesWrapper(it) }
                }
                return cache!!
            }
        }

    private operator fun get(i: Int): JFCValuesWrapper {
        return data[i]
    }

    override fun getSeriesKey(i: Int): Comparable<*> {
        return if (seriesCount == 1) {
            plot.name
        } else {
            Name.joinString(plot.name, Adapters.getTitle(adapter, Adapters.Y_AXIS))
        }
    }

    override fun getX(i: Int, i1: Int): Number? = this[i1].x

    override fun getY(i: Int, i1: Int): Number? = this[i1].y

    override fun getStartX(i: Int, i1: Int): Number? = this[i1].startX

    override fun getEndX(i: Int, i1: Int): Number? = this[i1].endX

    override fun getStartY(i: Int, i1: Int): Number? = this[i1].startY

    override fun getEndY(i: Int, i1: Int): Number? = this[i1].endY

    override fun getSeriesCount(): Int = 1

    override fun getItemCount(i: Int): Int {
        return data.size
    }
}
