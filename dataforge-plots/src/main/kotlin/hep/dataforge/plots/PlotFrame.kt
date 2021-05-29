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

import hep.dataforge.Type
import hep.dataforge.context.Plugin
import hep.dataforge.description.*
import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.io.envelopes.EnvelopeBuilder
import hep.dataforge.io.envelopes.JavaObjectWrapper.JAVA_SERIAL_DATA
import hep.dataforge.io.envelopes.SimpleEnvelope
import hep.dataforge.meta.*
import hep.dataforge.plots.PlotFrame.Companion.PLOT_FRAME_TYPE
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.data.XYFunctionPlot
import hep.dataforge.tables.ValuesAdapter
import hep.dataforge.utils.MetaFactory
import hep.dataforge.values.ValueType
import hep.dataforge.values.Values
import java.io.ObjectStreamException
import java.io.OutputStream
import java.io.Serializable

/**
 * Набор графиков (plot) в одном окошке (frame) с общими осями.
 *
 * @author Alexander Nozik
 */
@ValueDef(key = "title", info = "The title of the plot. By default the name of the Content is taken.")
@Type(PLOT_FRAME_TYPE)
interface PlotFrame : Configurable, Serializable {

    /**
     * Root plot node. Could be reassigned
     *
     * @return
     */
    val plots: PlotGroup

    /**
     * Add or replace registered plottable
     *
     * @param plotable
     */
    fun add(plotable: Plottable) {
        plots.add(plotable)
    }


    operator fun Plottable.unaryPlus() {
        this@PlotFrame.add(this)
    }

    /**
     * Add (replace) all plottables to the frame
     *
     * @param plottables
     */
    fun addAll(plottables: Iterable<Plottable>) {
        for (pl in plottables) {
            add(pl)
        }
    }

    /**
     * Update all plottables. Remove the ones not present in a new set
     *
     * @param plottables
     */
    fun setAll(plottables: Collection<Plottable>) {
        clear()
        plottables.forEach { this.add(it) }
    }

    /**
     * Remove plottable with given name
     *
     * @param plotName
     */
    fun remove(plotName: String) {
        plots.remove(plotName)
    }

    /**
     * Remove all plottables
     */
    fun clear() {
        plots.clear()
    }


    /**
     * Opt the plottable with the given name
     *
     * @param name
     * @return
     */
    operator fun get(name: String): Plottable?


    /**
     * Save plot as image
     *
     * @param stream
     * @param config
     */
    fun asImage(stream: OutputStream, config: Meta) {
        throw UnsupportedOperationException()
    }

    /**
     * Use exclusively for plot frame serialization
     */
    class PlotFrameEnvelope(envelope: Envelope) : SimpleEnvelope(envelope.meta, envelope.data) {

        @Throws(ObjectStreamException::class)
        private fun readResolve(): Any {
            return wrapper.unWrap(this)
        }
    }


    class Wrapper : hep.dataforge.io.envelopes.Wrapper<PlotFrame> {

        override val name: String
            get() = PLOT_FRAME_TYPE

        override val type: Class<PlotFrame>
            get() = PlotFrame::class.java

        override fun wrap(obj: PlotFrame): Envelope {
            val rootEnv = PlotGroup.WRAPPER.wrap(obj.plots)

            val builder = EnvelopeBuilder()
                    .meta(rootEnv.meta)
                    .data(rootEnv.data)
                    .setDataType(JAVA_SERIAL_DATA)
                    .setEnvelopeType(PLOT_FRAME_TYPE)
                    .setMetaValue(PLOT_FRAME_CLASS_KEY, obj.javaClass.name)
                    .putMetaNode(PLOT_FRAME_META_KEY, obj.config)
            return builder.build()
        }

        override fun unWrap(envelope: Envelope): PlotFrame {
            val root = PlotGroup.WRAPPER.unWrap(envelope)

            val plotFrameClassName = envelope.meta.getString(PLOT_FRAME_CLASS_KEY, "hep.dataforge.plots.JFreeChartFrame")
            val plotMeta = envelope.meta.getMetaOrEmpty(PLOT_FRAME_META_KEY)

            try {
                val frame = Class.forName(plotFrameClassName).getConstructor().newInstance() as PlotFrame
                frame.configure(plotMeta)
                frame.addAll(root)
                frame.plots.configure(root.config)

                return frame
            } catch (ex: Exception) {
                throw RuntimeException(ex)
            }

        }

        companion object {

            const val PLOT_FRAME_CLASS_KEY = "frame.class"
            const val PLOT_FRAME_META_KEY = "frame.meta"
        }

    }

    companion object {
        const val PLOT_FRAME_TYPE = "hep.dataforge.plotframe"
        val wrapper = Wrapper()
    }
}

interface PlotFactory : Plugin, MetaFactory<PlotFrame>

/**
 * Range for single numberic axis
 */
class Range(meta: Meta) : ConfigMorph(meta) {

    @Description("Lower boundary for fixed range")
    val from: Number by configDouble()

    @Description("Upper boundary for fixed range")
    val to: Number by configDouble()
}

/**
 * Axis definition
 */
class Axis(meta: Meta) : ConfigMorph(meta) {

    enum class Type {
        NUMBER,
        LOG,
        TIME
    }

    enum class Crosshair {
        NONE,
        FREE,
        DATA
    }

    @Description("The type of axis. By default number axis is used")
    @ValueProperty(enumeration = Type::class, def = "NUMBER")
    val type: Type by configEnum(def = Type.NUMBER)

    @Description("The title of the axis")
    @ValueProperty(type = [ValueType.STRING], def = "")
    val title: String by configString()

    @Description("The units of the axis")
    @ValueProperty(type = [ValueType.STRING], def = "")
    val units: String by configString(def = "")

    @Description("Appearance and type of the crosshair")
    @ValueProperty(enumeration = Crosshair::class, def = "NONE")
    val crosshair: Crosshair by configEnum(def = Crosshair.NONE)

    @Description("The definition of range for given axis")
    @NodeProperty
    @ValueDefs(
            ValueDef(key = "from", type = [ValueType.NUMBER], info = "Upper border for range"),
            ValueDef(key = "to", type = [ValueType.NUMBER], info = "Lower border for range")
    )
    val range: Range by morphConfigNode()
}

fun PlotFrame.plotData(name: String, data: Iterable<Values> = emptyList(), adapter: ValuesAdapter? = null, metaBuilder: KMetaBuilder.() -> Unit = {}) {
    add(DataPlot.plot(name, data, adapter, metaBuilder))
}

fun PlotFrame.plotFunction(name: String, from: Double, to: Double, numPoints: Int = XYFunctionPlot.DEFAULT_DENSITY, meta: Meta = Meta.empty(), function: (Double) -> Double) {
    add(XYFunctionPlot.plot(name, from, to, numPoints, meta, function))
}