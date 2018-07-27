package inr.numass.actions

import hep.dataforge.actions.OneToOneAction
import hep.dataforge.configure
import hep.dataforge.context.Context
import hep.dataforge.description.*
import hep.dataforge.maths.histogram.UnivariateHistogram
import hep.dataforge.meta.Laminate
import hep.dataforge.meta.buildMeta
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.output.plot
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.Table
import hep.dataforge.values.ValueType
import inr.numass.data.analyzers.NumassAnalyzer
import inr.numass.data.analyzers.TimeAnalyzer
import inr.numass.data.api.NumassPoint
import kotlin.streams.asStream

/**
 * Plot time analysis graphics
 */
@ValueDefs(
        ValueDef(key = "normalize", type = arrayOf(ValueType.BOOLEAN), def = "true", info = "Normalize t0 dependencies"),
        ValueDef(key = "t0", type = arrayOf(ValueType.NUMBER), def = "30e3", info = "The default t0 in nanoseconds"),
        ValueDef(key = "window.lo", type = arrayOf(ValueType.NUMBER), def = "500", info = "Lower boundary for amplitude window"),
        ValueDef(key = "window.up", type = arrayOf(ValueType.NUMBER), def = "10000", info = "Upper boundary for amplitude window"),
        ValueDef(key = "binNum", type = arrayOf(ValueType.NUMBER), def = "1000", info = "Number of bins for time histogram"),
        ValueDef(key = "binSize", type = arrayOf(ValueType.NUMBER), info = "Size of bin for time histogram. By default is defined automatically")
)
@NodeDefs(
        NodeDef(key = "histogram", info = "Configuration for  histogram plots"),
        NodeDef(key = "plot", info = "Configuration for stat plots")
)
@TypedActionDef(name = "numass.timeSpectrum", inputType = NumassPoint::class, outputType = Table::class)
object TimeSpectrumAction : OneToOneAction<NumassPoint, Table>() {
    private val analyzer = TimeAnalyzer();

    override fun execute(context: Context, name: String, input: NumassPoint, inputMeta: Laminate): Table {
        val log = getLog(context, name);


        val t0 = inputMeta.getDouble("t0", 30e3);
        val loChannel = inputMeta.getInt("window.lo", 500);
        val upChannel = inputMeta.getInt("window.up", 10000);


        val trueCR = analyzer.analyze(input, buildMeta {
            "t0" to t0
            "window.lo" to loChannel
            "window.up" to upChannel
        }).getDouble("cr")

        log.report("The expected count rate for 30 us delay is $trueCR")


        val binNum = inputMeta.getInt("binNum", 1000);
        val binSize = inputMeta.getDouble("binSize", 1.0 / trueCR * 10 / binNum * 1e6)

        val histogram = UnivariateHistogram.buildUniform(0.0, binSize * binNum, binSize)
                .fill(analyzer
                        .getEventsWithDelay(input, inputMeta)
                        .asStream()
                        .mapToDouble { it.second / 1000.0 }
                ).asTable()

        //.histogram(input, loChannel, upChannel, binSize, binNum).asTable();
        log.report("Finished histogram calculation...");

        if (inputMeta.getBoolean("plotHist", true)) {

            val histogramPlot = DataPlot(name)
                    .configure {
                        "showLine" to true
                        "showSymbol" to false
                        "showErrors" to false
                        "connectionType" to "step"
                        node("@adapter") {
                            "y.value" to "count"
                        }
                    }.apply { configure(inputMeta.getMetaOrEmpty("histogram")) }
                    .fillData(histogram)


            context.plot(histogramPlot, name, "histogram") {
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

            val statPlot = DataPlot(name).configure {
                "showLine" to true
                "thickness" to 4
                "title" to "${name}_${input.voltage}"
            }.apply {
                configure(inputMeta.getMetaOrEmpty("plot"))
            }

            context.plot(statPlot, name, "stat-method")

            (1..100).map { 1000 * it }.map { t ->
                val result = analyzer.analyze(input, buildMeta {
                    "t0" to t
                    "window.lo" to loChannel
                    "window.up" to upChannel
                })


                val norm = if (inputMeta.getBoolean("normalize", true)) {
                    trueCR
                } else {
                    1.0
                }

                statPlot.append(
                        Adapters.buildXYDataPoint(
                                t / 1000.0,
                                result.getDouble("cr") / norm,
                                result.getDouble(NumassAnalyzer.COUNT_RATE_ERROR_KEY) / norm
                        )
                )
            }


        }

        return histogram;
    }
}