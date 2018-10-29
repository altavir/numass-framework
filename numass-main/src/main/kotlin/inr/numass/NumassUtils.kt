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
package inr.numass

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.data.DataNode
import hep.dataforge.data.DataSet
import hep.dataforge.data.binary.Binary
import hep.dataforge.io.envelopes.DefaultEnvelopeType
import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.io.envelopes.EnvelopeBuilder
import hep.dataforge.io.envelopes.TaglessEnvelopeType
import hep.dataforge.io.output.StreamOutput
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import hep.dataforge.tables.ListTable
import hep.dataforge.tables.Table
import hep.dataforge.values.ValueMap
import hep.dataforge.values.Values
import inr.numass.data.analyzers.NumassAnalyzer
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import inr.numass.models.FSS
import inr.numass.utils.ExpressionUtils
import kotlinx.coroutines.experimental.runBlocking
import org.apache.commons.math3.analysis.UnivariateFunction
import org.jfree.chart.plot.IntervalMarker
import org.jfree.chart.ui.RectangleInsets
import org.slf4j.Logger
import tornadofx.*
import java.awt.Color
import java.awt.Font
import java.io.IOException
import java.io.OutputStream
import java.lang.Math.*
import java.util.*

/**
 * @author Darksnake
 */
object NumassUtils {

    /**
     * Integral beta spectrum background with given amplitude (total count rate
     * from)
     *
     * @param amplitude
     * @return
     */
    fun tritiumBackgroundFunction(amplitude: Double): UnivariateFunction {
        return UnivariateFunction { e: Double ->
            /*чистый бета-спектр*/
            val e0 = 18575.0
            val D = e0 - e//E0-E
            if (D <= 0) {
                0.0
            } else {
                amplitude * factor(e) * D * D
            }
        }
    }

    private fun factor(E: Double): Double {
        val me = 0.511006E6
        val Etot = E + me
        val pe = sqrt(E * (E + 2.0 * me))
        val ve = pe / Etot
        val yfactor = 2.0 * 2.0 * 1.0 / 137.039 * Math.PI
        val y = yfactor / ve
        val Fn = y / abs(1.0 - exp(-y))
        val Fermi = Fn * (1.002037 - 0.001427 * ve)
        val res = Fermi * pe * Etot
        return res * 1E-23
    }

    fun <T> wrap(obj: T, meta: Meta = Meta.empty(), serializer: OutputStream.(T) -> Unit): EnvelopeBuilder {
        return EnvelopeBuilder().meta(meta).data { serializer.invoke(it, obj) }
    }

    fun wrap(obj: Any, meta: Meta = Meta.empty()): EnvelopeBuilder {
        return wrap(obj, meta) { StreamOutput(Global, this).render(it, meta) }
    }


    /**
     * Write an envelope wrapping given data to given stream
     *
     * @param stream
     * @param meta
     * @param dataWriter
     * @throws IOException
     */
    fun writeEnvelope(stream: OutputStream, meta: Meta, dataWriter: (OutputStream) -> Unit) {
        try {
            TaglessEnvelopeType.INSTANCE.writer.write(
                    stream,
                    EnvelopeBuilder()
                            .meta(meta)
                            .data(dataWriter)
                            .build()
            )
            stream.flush()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }

    fun writeEnvelope(stream: OutputStream, envelope: Envelope) {
        try {
            DefaultEnvelopeType.INSTANCE.writer.write(stream, envelope)
            stream.flush()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }

//    fun write(stream: OutputStream, meta: Meta, something: Markedup) {
//        writeEnvelope(stream, meta) { out ->
//            SimpleMarkupRenderer(out).render(something.markup(meta))
//        }
//    }

    /**
     * Convert numass set to DataNode
     *
     * @param set
     * @return
     */
    fun setToNode(set: NumassSet): DataNode<Any> {
        val builder = DataSet.edit()
        builder.name = set.name
        set.points.forEach { point ->
            val pointMeta = MetaBuilder("point")
                    .putValue("voltage", point.voltage)
                    .putValue("index", point.meta.getInt("external_meta.point_index", -1))
                    .putValue("run", point.meta.getString("external_meta.session", ""))
                    .putValue("group", point.meta.getString("external_meta.group", ""))
            val pointName = "point_" + point.meta.getInt("external_meta.point_index", point.hashCode())
            builder.putData(pointName, point, pointMeta)
        }
        runBlocking {
            set.getHvData()?.let { hv -> builder.putData("hv", hv, Meta.empty()) }
        }
        return builder.build()
    }

    /**
     * Convert numass set to uniform node which consists of points
     *
     * @param set
     * @return
     */
    fun pointsToNode(set: NumassSet): DataNode<NumassPoint> {
        return setToNode(set).checked(NumassPoint::class.java)
    }

}

fun getFSS(context: Context, meta: Meta): FSS? {
    return if (meta.getBoolean("useFSS", true)) {
        val fssBinary: Binary? = meta.optString("fssFile")
                .map { fssFile -> context.getFile(fssFile).binary }
                .orElse(context.getResource("data/FS.txt"))
        fssBinary?.let { FSS(it.stream) } ?: throw RuntimeException("Could not load FSS file")
    } else {
        null
    }
}


/**
 * Evaluate groovy expression using numass point as parameter
 *
 * @param expression
 * @param point
 * @return
 */
fun pointExpression(expression: String, point: Values): Double {
    val exprParams = HashMap<String, Any>()
    //Adding all point values to expression parameters
    point.names.forEach { name -> exprParams[name] = point.getValue(name).value }
    //Adding aliases for commonly used parameters
    exprParams["T"] = point.getDouble("length")
    exprParams["U"] = point.getDouble("voltage")

    return ExpressionUtils.function(expression, exprParams)
}

/**
 * Add set markers to time chart
 */
fun JFreeChartFrame.addSetMarkers(sets: Collection<NumassSet>) {
    val jfcPlot = chart.xyPlot
    val paint = Color(0.0f, 0.0f, 1.0f, 0.1f)
    sets.stream().forEach { set ->
        val start = set.startTime;
        val stop = set.meta.optValue("end_time").map { it.time }
                .orElse(start.plusSeconds(300))
                .minusSeconds(60)
        val marker = IntervalMarker(start.toEpochMilli().toDouble(), stop.toEpochMilli().toDouble(), paint)
        marker.label = set.name
        marker.labelFont = Font("Verdana", Font.BOLD, 20);
        marker.labelOffset = RectangleInsets(30.0, 30.0, 30.0, 30.0)
        runLater { jfcPlot.addDomainMarker(marker) }
    }
}

/**
 * Subtract one U spectrum from the other one
 */
fun subtractSpectrum(merge: Table, empty: Table, logger: Logger? = null): Table {
    val builder = ListTable.Builder(merge.format)
    merge.rows.forEach { point ->
        val pointBuilder = ValueMap.Builder(point)
        val referencePoint = empty.rows
                .filter { p -> Math.abs(p.getDouble(NumassPoint.HV_KEY) - point.getDouble(NumassPoint.HV_KEY)) < 0.1 }.findFirst()
        if (referencePoint.isPresent) {
            pointBuilder.putValue(
                    NumassAnalyzer.COUNT_RATE_KEY,
                    Math.max(0.0, point.getDouble(NumassAnalyzer.COUNT_RATE_KEY) - referencePoint.get().getDouble(NumassAnalyzer.COUNT_RATE_KEY))
            )
            pointBuilder.putValue(
                    NumassAnalyzer.COUNT_RATE_ERROR_KEY,
                    Math.sqrt(Math.pow(point.getDouble(NumassAnalyzer.COUNT_RATE_ERROR_KEY), 2.0) + Math.pow(referencePoint.get().getDouble(NumassAnalyzer.COUNT_RATE_ERROR_KEY), 2.0)))
        } else {
            logger?.warn("No reference point found for voltage = {}", point.getDouble(NumassPoint.HV_KEY))
        }
        builder.row(pointBuilder.build())
    }

    return builder.build()
}

fun Values.unbox(): Map<String, Any?> {
    val res = HashMap<String, Any?>()
    for (field in this.names) {
        val value = this.getValue(field)
        res[field] = value.value
    }
    return res
}

//fun FitResult.display(context: Context, stage: String = "fit") {
//    val model = optModel(context).get() as XYModel
//
//    val adapter = model.adapter
//
//    val frame = PlotUtils.getPlotManager(context)
//            .getPlotFrame(stage, "plot", Meta.empty())
//
//    val func = { x: Double -> model.spectrum.value(x, parameters) }
//
//    val fit = XYFunctionPlot("fit", function = func)
//    fit.density = 100
//    // ensuring all data points are calculated explicitly
//    data.rows.map { dp -> Adapters.getXValue(adapter, dp).doubleValue() }.sorted().forEach { fit.calculateIn(it) }
//
//    frame.add(fit)
//
//    frame.add(DataPlot.plot("data", adapter, data))
//
//    val residualsFrame = PlotUtils.getPlotManager(context)
//            .getPlotFrame(stage, "residuals", Meta.empty())
//
//    val residual = DataPlot("residuals");
//
//    data.rows.forEach {
//        val x = Adapters.getXValue(adapter, it).doubleValue()
//        val y = Adapters.getYValue(adapter, it).doubleValue()
//        val err = Adapters.optYError(adapter, it).orElse(1.0)
//        residual += Adapters.buildXYDataPoint(x, (y - func(x)) / err, 1.0)
//    }
//
//    residualsFrame.add(residual)
//
//}
