package inr.numass.actions

import hep.dataforge.actions.OneToOneAction
import hep.dataforge.configure
import hep.dataforge.context.Context
import hep.dataforge.description.*
import hep.dataforge.maths.histogram.UnivariateHistogram
import hep.dataforge.meta.Laminate
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.data.XYFunctionPlot
import hep.dataforge.plots.output.plot
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.Table
import hep.dataforge.values.ValueType
import inr.numass.data.analyzers.NumassAnalyzer
import inr.numass.data.analyzers.TimeAnalyzer
import inr.numass.data.analyzers.TimeAnalyzer.Companion.T0_KEY
import inr.numass.data.api.NumassPoint
import kotlin.streams.asStream

/**
 * Plot time analysis graphics
 */
@ValueDefs(
        ValueDef(key = "normalize", type = arrayOf(ValueType.BOOLEAN), def = "false", info = "Normalize t0 dependencies"),
        ValueDef(key = "t0", type = arrayOf(ValueType.NUMBER), def = "30e3", info = "The default t0 in nanoseconds"),
        ValueDef(key = "window.lo", type = arrayOf(ValueType.NUMBER), def = "0", info = "Lower boundary for amplitude window"),
        ValueDef(key = "window.up", type = arrayOf(ValueType.NUMBER), def = "10000", info = "Upper boundary for amplitude window"),
        ValueDef(key = "binNum", type = arrayOf(ValueType.NUMBER), def = "1000", info = "Number of bins for time histogram"),
        ValueDef(key = "binSize", type = arrayOf(ValueType.NUMBER), info = "Size of bin for time histogram. By default is defined automatically")
)
@NodeDefs(
        NodeDef(key = "histogram", info = "Configuration for  histogram plots"),
        NodeDef(key = "plot", info = "Configuration for stat plots")
)
@TypedActionDef(name = "timeSpectrum", inputType = NumassPoint::class, outputType = Table::class)
object TimeAnalyzerAction : OneToOneAction<NumassPoint, Table>("timeSpectrum",NumassPoint::class.java,Table::class.java) {
    private val analyzer = TimeAnalyzer();

    override fun execute(context: Context, name: String, input: NumassPoint, inputMeta: Laminate): Table {
        val log = getLog(context, name);

        val analyzerMeta = inputMeta.getMetaOrEmpty("analyzer")

        val initialEstimate = analyzer.analyze(input, analyzerMeta)
        val cr = initialEstimate.getDouble("cr")

        log.report("The expected count rate for ${initialEstimate.getDouble(T0_KEY)} us delay is $cr")

        val binNum = inputMeta.getInt("binNum", 1000);
        val binSize = inputMeta.getDouble("binSize", 1.0 / cr * 10 / binNum * 1e6)

        val histogram = UnivariateHistogram.buildUniform(0.0, binSize * binNum, binSize)
                .fill(analyzer
                        .getEventsWithDelay(input, inputMeta)
                        .asStream()
                        .mapToDouble { it.second.toDouble() / 1000.0 }
                ).asTable()

        //.histogram(input, loChannel, upChannel, binSize, binNum).asTable();
        log.report("Finished histogram calculation...");

        if (inputMeta.getBoolean("plotHist", true)) {

            val histogramPlot = DataPlot(name, adapter = Adapters.buildXYAdapter("x", "count"))
                    .configure {
                        "showLine" to true
                        "showSymbol" to false
                        "showErrors" to false
                        "connectionType" to "step"
                    }.apply {
                        configure(inputMeta.getMetaOrEmpty("histogram"))
                    }.fillData(histogram)


            val functionPlot = XYFunctionPlot.plot(name + "_theory", 0.0, binSize * binNum) {
                cr / 1e6 * initialEstimate.getInt(NumassAnalyzer.COUNT_KEY) * binSize * Math.exp(-it * cr / 1e6)
            }

            context.plot(listOf(histogramPlot, functionPlot), name = "histogram", stage = this.name) {
                "xAxis" to {
                    "title" to "delay"
                    "units" to "us"
                }
                "yAxis" to {
                    "type" to "log"
                }
            }
        }

        if (inputMeta.getBoolean("plotStat", true)) {

            val statPlot = DataPlot(name, adapter = Adapters.DEFAULT_XYERR_ADAPTER).configure {
                "showLine" to true
                "thickness" to 4
                "title" to "${name}_${input.voltage}"
                update(inputMeta.getMetaOrEmpty("plot"))
            }

            val errorPlot = DataPlot(name).configure{
                "showLine" to true
                "showErrors" to false
                "showSymbol" to false
                "thickness" to 4
                "title" to "${name}_${input.voltage}"
            }

            context.plot(statPlot, name = "count rate", stage = this.name) {
                "xAxis" to {
                    "title" to "delay"
                    "units" to "us"
                }
                "yAxis" to {
                    "title" to "Reconstructed count rate"
                }
            }

            context.plot(errorPlot, name = "error", stage = this.name){
                "xAxis" to {
                    "title" to "delay"
                    "units" to "us"
                }
                "yAxis" to {
                    "title" to "Statistical error"
                }
            }

            val minT0 = inputMeta.getDouble("t0.min", 0.0)
            val maxT0 = inputMeta.getDouble("t0.max", 1e9 / cr)
            val steps = inputMeta.getInt("t0.steps", 100)

            val norm = if (inputMeta.getBoolean("normalize", false)) {
                cr
            } else {
                1.0
            }

            (0..steps).map { minT0 + (maxT0 - minT0) / steps * it }.map { t ->
                val result = analyzer.analyze(input, analyzerMeta.builder.setValue("t0", t))

                if (Thread.currentThread().isInterrupted) {
                    throw InterruptedException()
                }
                statPlot.append(
                        Adapters.buildXYDataPoint(
                                t / 1000.0,
                                result.getDouble("cr") / norm,
                                result.getDouble(NumassAnalyzer.COUNT_RATE_ERROR_KEY) / norm
                        )
                )

                errorPlot.append(
                        Adapters.buildXYDataPoint(t/1000.0, result.getDouble(NumassAnalyzer.COUNT_RATE_ERROR_KEY) / norm)
                )
            }


        }

        return histogram;
    }
}