/*
 * Copyright  2018 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package hep.dataforge.plots

import hep.dataforge.meta.Laminate
import hep.dataforge.meta.SimpleConfigurable
import hep.dataforge.names.Name

/**
 * @author Alexander Nozik
 */
abstract class AbstractPlotFrame : SimpleConfigurable(), PlotFrame, PlotListener {

    final override val plots by lazy {
        PlotGroup("").apply { addListener(this@AbstractPlotFrame, false) }
    }

    override fun get(name: String): Plottable? {
        return plots[name]
    }

    /**
     * Reload data for plottable with given name.
     *
     * @param name
     */
    protected abstract fun updatePlotData(name: Name, plot: Plottable?)

    /**
     * Reload an annotation for given plottable.
     *
     * @param name
     */
    protected abstract fun updatePlotConfig(name: Name, config: Laminate)

    private fun resolveMeta(root: Plottable, path: Name): Laminate {
        return if (path.isEmpty()) {
            Laminate(listOf(root.config), root.descriptor)
        } else {
            val plot = (root as? PlotGroup)?.get(path.first)
            if (plot == null) {
                throw IllegalStateException("Can't resolve plottable with name $path in $root.")
            } else {
                resolveMeta(plot, path.cutFirst()) + root.config
            }
        }
    }


    override fun dataChanged(caller: Plottable, path: Name, before: Plottable?, after: Plottable?) {
        if (caller === this.plots) {
            updatePlotData(path, after)
            (before as? PlotGroup)?.stream(true)?.forEach {
                dataChanged(caller, path + it.first, it.second , (after as? PlotGroup)?.get(it.first))
            }
            (after as? PlotGroup)?.stream(true)?.forEach {
                dataChanged(caller, path + it.first, (before as? PlotGroup)?.get(it.first) , it.second)
            }
        } else {
            error("The caller is expected to be this frame plots")
        }
    }

    override fun metaChanged(caller: Plottable, path: Name, plot: Plottable) {
        updatePlotConfig(path, resolveMeta(caller, path))
        if(plot is PlotGroup){
            plot.stream(true).forEach{
                val childPath = path + it.first
                updatePlotConfig(childPath, resolveMeta(caller, childPath))
            }
        }

    }

}
