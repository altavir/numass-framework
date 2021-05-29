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
import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.io.envelopes.EnvelopeBuilder
import hep.dataforge.io.envelopes.JavaObjectWrapper.JAVA_SERIAL_DATA
import hep.dataforge.io.envelopes.Wrapper.Companion.WRAPPER_CLASS_KEY
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaNode.DEFAULT_META_NAME
import hep.dataforge.names.AnonymousNotAlowed
import hep.dataforge.plots.Plot.Companion.PLOT_TYPE
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.tables.ListOfPoints
import hep.dataforge.tables.ValuesAdapter
import hep.dataforge.values.Value
import hep.dataforge.values.Values
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * Единичный набор данных для отрисовки
 *
 * @author Alexander Nozik
 */
@AnonymousNotAlowed
@Type(PLOT_TYPE)
interface Plot : Plottable {
    /**
     * Get the whole data set without limitations
     *
     * @return
     */
    val data: List<Values>
        get() = getData(Meta.empty())

    /**
     * Get current adapter for this plottable
     *
     * @return
     */
    val adapter: ValuesAdapter

    /**
     * Get immutable list of data data according to query
     *
     * @param query
     * @return
     */
    fun getData(query: Meta): List<Values>

    fun getComponent(index: Int, component: String): Value {
        return adapter.getComponent(data[index], component)
    }

    class Wrapper : hep.dataforge.io.envelopes.Wrapper<Plot> {

        override val name: String
            get() = PLOT_TYPE

        override val type: Class<Plot>
            get() = Plot::class.java

        override fun wrap(obj: Plot): Envelope {
            val builder = EnvelopeBuilder()
                    .setEnvelopeType(PLOT_TYPE)
                    .setDataType(JAVA_SERIAL_DATA)
                    .setMetaValue(WRAPPER_CLASS_KEY, javaClass.name)
                    .setMetaValue("name", obj.name)
                    //.putMetaNode("descriptor", plot.getDescriptor().toMeta())
                    .putMetaNode(DEFAULT_META_NAME, obj.config)


            val baos = ByteArrayOutputStream()
            try {
                ObjectOutputStream(baos).use { os -> os.writeObject(ListOfPoints(obj.data)) }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }

            builder.data(baos.toByteArray())
            return builder
        }

        override fun unWrap(envelope: Envelope): Plot {
            try {
                val meta = envelope.meta.getMetaOrEmpty(DEFAULT_META_NAME)
                val name = envelope.meta.getString("name")

                val data = ObjectInputStream(envelope.data.stream).readObject() as ListOfPoints

                //Restore always as plottableData
                val pl = DataPlot(name, meta)
                pl.fillData(data)
                return pl
            } catch (ex: Exception) {
                throw RuntimeException("Failed to read Plot", ex)
            }

        }



    }

    companion object {
        const val PLOT_TYPE = Plottable.PLOTTABLE_TYPE + ".plot"
    }

}
