/*
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hep.dataforge.plots.data

import hep.dataforge.meta.Meta
import hep.dataforge.meta.SimpleConfigurable
import hep.dataforge.names.Name
import hep.dataforge.plots.Plot
import hep.dataforge.plots.PlotListener
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.ValuesAdapter
import hep.dataforge.utils.ReferenceRegistry

/**
 * @author darksnake
 */
abstract class AbstractPlot(
        override val name: String,
        meta: Meta,
        adapter: ValuesAdapter?
) : SimpleConfigurable(meta), Plot {

    override val adapter: ValuesAdapter = adapter ?: Adapters.buildAdapter(meta.getMetaOrEmpty("adapter"))

    private val listeners = ReferenceRegistry<PlotListener>()
    override fun addListener(listener: PlotListener, isStrong: Boolean) {
        listeners.add(listener, true)
    }

    override fun removeListener(listener: PlotListener) {
        this.listeners.remove(listener)
    }


    /**
     * Notify all listeners that configuration changed
     *
     * @param config
     */
    @Synchronized
    override fun applyConfig(config: Meta) {
        listeners.forEach { l -> l.metaChanged(this, Name.empty(),this) }
    }

    /**
     * Notify all listeners that data changed
     */
    @Synchronized
    fun notifyDataChanged() {
        listeners.forEach { l -> l.dataChanged(this, Name.empty(), this, this) }
    }
}
