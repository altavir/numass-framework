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

import hep.dataforge.description.Descriptors
import hep.dataforge.description.NodeDescriptor
import hep.dataforge.io.envelopes.DefaultEnvelopeType
import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.io.envelopes.EnvelopeBuilder
import hep.dataforge.io.envelopes.EnvelopeType
import hep.dataforge.io.envelopes.JavaObjectWrapper.JAVA_SERIAL_DATA
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaNode.DEFAULT_META_NAME
import hep.dataforge.meta.SimpleConfigurable
import hep.dataforge.names.Name
import hep.dataforge.providers.Provider
import hep.dataforge.providers.Provides
import hep.dataforge.providers.ProvidesNames
import hep.dataforge.utils.ReferenceRegistry
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.stream.Stream

/**
 * A group of plottables. It could store Plots as well as other plot groups.
 */
class PlotGroup(override  val name: String, descriptor: NodeDescriptor = NodeDescriptor.empty("group"))
    : SimpleConfigurable(), Plottable, Provider, PlotListener, Iterable<Plottable> {

    private val plots = LinkedHashSet<Plottable>()
    private val listeners = ReferenceRegistry<PlotListener>()

    override var descriptor = descriptor
        set(value) {
            field = value
            metaChanged(this, Name.empty(), this)
        }

    private fun resolvePlotName(caller: Plottable): Name {
        return when {
            caller == this -> Name.empty()
            plots.contains(caller) -> Name.ofSingle(caller.name)
            else -> error("Could not find $caller in $this")
        }
    }

    override fun dataChanged(caller: Plottable, path: Name, before: Plottable?, after: Plottable?) {
        listeners.forEach {
            it.dataChanged(this, resolvePlotName(caller) + path, before, after)
        }
    }

    override fun metaChanged(caller: Plottable, path: Name, plot: Plottable) {
        listeners.forEach { it.metaChanged(this, resolvePlotName(caller) + path, plot) }
    }


    fun add(plot: Plottable) {
        val before = plots.find { it.name == plot.name }
        this.plots.add(plot)
        dataChanged(this, Name.ofSingle(plot.name), before, plot)
        plot.addListener(this)
    }

    operator fun Plottable.unaryPlus() {
        this@PlotGroup.add(this)
    }

    /**
     * Recursive remove a plot
     *
     * @param name
     * @return
     */
    fun remove(name: String): PlotGroup {
        return remove(Name.of(name))
    }

    fun remove(name: Name): PlotGroup {
        if (name.length == 1) {
            val plot = plots.find { it.name == name.unescaped }
            if (plot != null) {
                plots.remove(plot)
                dataChanged(this, name, plot, null)
                plot.removeListener(this)
            }
        } else {
            (get(name.cutLast()) as? PlotGroup)?.remove(name.last)
        }
        return this
    }

    fun clear() {
        plots.forEach { it.removeListener(this) }
        plots.clear()
        dataChanged(this, Name.empty(), this, this)
    }

    @ProvidesNames(PLOT_TARGET)
    fun list(): Stream<String> = stream().map { it.first.toString() }

    /**
     * Recursive stream of all plots excluding intermediate nodes
     *
     * @return
     */
    fun stream(recursive: Boolean = true): Stream<Pair<Name, Plottable>> = plots.stream().flatMap {
        if (recursive && it is PlotGroup) {
            it.stream().map { pair -> Pair(Name.ofSingle(it.name) + pair.first, pair.second) }
        } else {
            Stream.of(Pair(Name.ofSingle(it.name), it))
        }
    }

    @Provides(PLOT_TARGET)
    operator fun get(name: String): Plottable? = get(Name.of(name))

    operator fun get(name: Name): Plottable? = when (name.length) {
        0 -> this
        1 -> plots.find { it.name == name.unescaped }
        else -> (get(name.cutLast()) as? PlotGroup)?.get(name.last)
    }

    /**
     * * Add plottable if it is absent,
     * * Remove it if null,
     * * Replace if present and not same,
     * * Do nothing if present and same
     */
    operator fun set(name: Name, plot: Plottable?) {
        if (plot == null) {
            remove(name)
        } else {
            val current = get(name)
            if (current == null) {
                (get(name.cutLast()) as? PlotGroup)?.add(plot)
            } else if (current !== plot) {
                remove(name)
                (get(name.cutLast()) as? PlotGroup)?.add(plot)
            }
        }
    }

    /**
     * Add plottable state listener
     *
     * @param listener
     */
    override fun addListener(listener: PlotListener, isStrong: Boolean) {
        listeners.add(listener, isStrong)
    }

    /**
     * Remove plottable state listener
     *
     * @param listener
     */
    override fun removeListener(listener: PlotListener) {
        listeners.remove(listener)
    }


    override fun applyConfig(config: Meta) {
        super.applyConfig(config)
        metaChanged(this, Name.empty(), this)
    }


    fun setType(type: Class<out Plottable>) {
        descriptor = Descriptors.forType("plot", type.kotlin)
        configureValue("@descriptor", "class::${type.name}")
    }

    inline fun <reified T : Plottable> setType() {
        setType(T::class.java)
    }

    /**
     * Iterate over direct descendants
     *
     * @return
     */
    override fun iterator(): Iterator<Plottable> = this.plots.iterator()

    class Wrapper : hep.dataforge.io.envelopes.Wrapper<PlotGroup> {

        override val name: String
            get() = PLOT_GROUP_WRAPPER_TYPE

        override val type: Class<PlotGroup>
            get() = PlotGroup::class.java

        override fun wrap(obj: PlotGroup): Envelope {
            val baos = ByteArrayOutputStream()
            val writer = DefaultEnvelopeType.INSTANCE.writer

            for (plot in obj.plots) {
                try {
                    val env: Envelope = when (plot) {
                        is PlotGroup -> wrap(plot)
                        is Plot -> plotWrapper.wrap(plot)
                        else -> throw RuntimeException("Unknown plottable type")
                    }
                    writer.write(baos, env)
                } catch (ex: IOException) {
                    throw RuntimeException("Failed to write plot group to envelope", ex)
                }

            }

            val builder = EnvelopeBuilder()
                    .setMetaValue("name", obj.name)
                    .putMetaNode(DEFAULT_META_NAME, obj.config)
                    .setEnvelopeType(PLOT_GROUP_WRAPPER_TYPE)
                    .setDataType(JAVA_SERIAL_DATA)
                    .data(baos.toByteArray())

            builder.putMetaNode("descriptor", obj.descriptor.toMeta())
            return builder.build()
        }

        override fun unWrap(envelope: Envelope): PlotGroup {
            //checkValidEnvelope(envelope);
            val groupName = envelope.meta.getString("name")
            val groupMeta = envelope.meta.getMetaOrEmpty(DEFAULT_META_NAME)
            val group = PlotGroup(groupName)
            group.configure(groupMeta)

            val internalEnvelopeType = EnvelopeType.resolve(envelope.meta.getString("@envelope.internalType", "default"))

            try {
                val dataStream = envelope.data.stream

                while (dataStream.available() > 0) {
                    val item = internalEnvelopeType!!.reader.read(dataStream)
                    try {
                        val pl = Plottable::class.java.cast(hep.dataforge.io.envelopes.Wrapper.unwrap(item))
                        group.add(pl)
                    } catch (ex: Exception) {
                        LoggerFactory.getLogger(javaClass).error("Failed to unwrap plottable", ex)
                    }

                }

                return group
            } catch (ex: Exception) {
                throw RuntimeException(ex)
            }

        }

        companion object {
            const val PLOT_GROUP_WRAPPER_TYPE = "hep.dataforge.plots.group"
            private val plotWrapper = Plot.Wrapper()
        }
    }

    companion object {
        const val PLOT_TARGET = "plot"

        inline fun <reified T : Plottable> typed(name: String): PlotGroup {
            return PlotGroup(name, Descriptors.forType("plot", T::class))
        }

        val WRAPPER = Wrapper()
    }

}