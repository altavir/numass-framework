package inr.numass.actions

import hep.dataforge.actions.OneToOneAction
import hep.dataforge.context.Context
import hep.dataforge.description.*
import hep.dataforge.kodex.configure
import hep.dataforge.maths.histogram.UnivariateHistogram
import hep.dataforge.meta.Laminate
import hep.dataforge.plots.PlotPlugin
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.Table
import hep.dataforge.values.ValueType
import inr.numass.data.analyzers.NumassAnalyzer
import inr.numass.data.analyzers.TimeAnalyzer
import inr.numass.data.api.NumassPoint

/**
 * Plot time analysis graphics
 */
@ValueDefs(
        ValueDef(name = "normalize", type = arrayOf(ValueType.BOOLEAN), def = "true", info = "Normalize t0 dependencies"),
        ValueDef(name = "t0", type = arrayOf(ValueType.NUMBER), def = "30e3", info = "The default t0 in nanoseconds"),
        ValueDef(name = "window.lo", type = arrayOf(ValueType.NUMBER), def = "0", info = "Lower boundary for amplitude window"),
        ValueDef(name = "window.up", type = arrayOf(ValueType.NUMBER), def = "10000", info = "Upper boundary for amplitude window"),
        ValueDef(name = "binNum", type = arrayOf(ValueType.NUMBER), def = "1000", info = "Number of bins for time histogram"),
        ValueDef(name = "binSize", type = arrayOf(ValueType.NUMBER), info = "Size of bin for time histogram. By default is defined automatically")
)
@NodeDefs(
        NodeDef(name = "histogram", info = "Configuration for  histogram plots"),
        NodeDef(name = "plot", info = "Configuration for stat plots")
)
@TypedActionDef(name = "timeSpectrum", inputType = NumassPoint::class, outputType = Table::class)
class TimeAnalyzerAction : OneToOneAction<NumassPoint, Table>() {
    private val analyzer = TimeAnalyzer();

    override fun execute(context: Context, name: String, input: NumassPoint, inputMeta: Laminate): Table {
        val log = getLog(context, name);


        //val t0 = inputMeta.getDouble("t0", 30e3);
//        val loChannel = inputMeta.getInt("window.lo", 500);
//        val upChannel = inputMeta.getInt("window.up", 10000);
        val pm = context.get(PlotPlugin::class.java);


        val trueCR = analyzer.analyze(input, inputMeta).getDouble("cr")

        log.report("The expected count rate for 30 us delay is $trueCR")


        val binNum = inputMeta.getInt("binNum", 1000);
        val binSize = inputMeta.getDouble("binSize", 1.0 / trueCR * 10 / binNum * 1e6)

        val histogram = UnivariateHistogram.buildUniform(0.0, binSize * binNum, binSize)
                .fill(analyzer
                        .getEventsWithDelay(input, inputMeta)
                        .mapToDouble { it.second / 1000.0 }
                ).asTable()

        //.histogram(input, loChannel, upChannel, binSize, binNum).asTable();
        log.report("Finished histogram calculation...");

        if (inputMeta.getBoolean("plotHist", true)) {

            val histPlot = pm.getPlotFrame(name, "histogram");

            histPlot.configure {
                node("xAxis") {
                    "title" to "delay"
                    "units" to "us"
                }
                node("yAxis") {
                    "type" to "log"
                }
            }

            val histogramPlot = DataPlot(name, adapter = Adapters.buildXYAdapter("x", "count"))
                    .configure {
                        "showLine" to true
                        "showSymbol" to false
                        "showErrors" to false
                        "connectionType" to "step"
                    }.apply {
                configure(inputMeta.getMetaOrEmpty("histogram"))
            }.fillData(histogram)

            histPlot.add(histogramPlot)
        }

        if (inputMeta.getBoolean("plotStat", true)) {

            val statPlot = DataPlot(name).configure {
                "showLine" to true
                "thickness" to 4
                "title" to "${name}_${input.voltage}"
            }.apply {
                configure(inputMeta.getMetaOrEmpty("plot"))
            }

            pm.getPlotFrame(name, "stat-method").add(statPlot)

            (1..100).map { inputMeta.getDouble("t0Step", 1000.0) * it }.map { t ->
                val result = analyzer.analyze(input, inputMeta.builder.setValue("t0", t))


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