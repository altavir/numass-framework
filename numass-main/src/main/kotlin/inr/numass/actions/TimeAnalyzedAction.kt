package inr.numass.actions

import hep.dataforge.actions.OneToOneAction
import hep.dataforge.context.Context
import hep.dataforge.description.TypedActionDef
import hep.dataforge.kodex.buildMeta
import hep.dataforge.kodex.configure
import hep.dataforge.maths.histogram.UnivariateHistogram
import hep.dataforge.meta.Laminate
import hep.dataforge.plots.PlotManager
import hep.dataforge.plots.data.PlottableData
import hep.dataforge.tables.Table
import hep.dataforge.tables.ValueMap
import inr.numass.data.analyzers.TimeAnalyzer
import inr.numass.data.api.NumassAnalyzer
import inr.numass.data.api.NumassPoint

/**
 * Plot time analysis graphics
 */
@TypedActionDef(name = "timeSpectrum", inputType = NumassPoint::class, outputType = Table::class)
class TimeAnalyzedAction : OneToOneAction<NumassPoint, Table>() {
    private val analyzer = TimeAnalyzer();

    override fun execute(context: Context, name: String, input: NumassPoint, inputMeta: Laminate): Table {
        val log = getLog(context, name);

        val loChannel = inputMeta.getInt("window.lo", 500);
        val upChannel = inputMeta.getInt("window.up", 10000);
        val pm = context.getFeature(PlotManager::class.java);

        //TODO use meta parameters

        val trueCR = analyzer.analyze(input, buildMeta {
            "t0" to 30e3
            "window.lo" to loChannel
            "window.up" to upChannel
        }).getDouble("cr")

        val binNum = inputMeta.getInt("binNum", 1000);
        val binSize = inputMeta.getDouble("binSize", 1.0 / trueCR * 10 / binNum)

        val histogram = UnivariateHistogram.buildUniform(0.0, binSize * binNum, binSize)
                .fill(analyzer
                        .getEventsWithDelay(input, inputMeta)
                        .mapToDouble { it.value / 1000.0 }
                ).asTable()

        //.histogram(input, loChannel, upChannel, binSize, binNum).asTable();
        log.report("Finished histogram calculation...");

        val histPlot = pm.getPlotFrame(getName(), "histogram");

        histPlot.configure {
            node("xAxis") {
                "axisTitle" to "delay"
                "axisUnits" to "us"
            }
            node("xAxis") {
                "type" to "log"
            }
        }

        histPlot.add(PlottableData(name)
                .configure {
                    "showLine" to true
                    "showSymbol" to false
                    "showErrors" to false
                    "connectionType" to "step"
                    node("adapter") {
                        "y.value" to "count"
                    }
                }.fillData(histogram)
        )

        log.report("The expected count rate for 30 us delay is $trueCR")

        val statPlotPoints = (1..150).map { 1000 * it }.map { t0 ->
            val result = analyzer.analyze(input, buildMeta {
                "t0" to t0
                "window.lo" to loChannel
                "window.up" to upChannel
            })
            ValueMap.ofMap(
                    mapOf(
                            "x" to t0 / 1000,
                            "y" to result.getDouble("cr"),
                            "y.err" to result.getDouble(NumassAnalyzer.COUNT_RATE_ERROR_KEY)
                    )
            );
        }

        pm.getPlotFrame(getName(), "stat-method").add(
                PlottableData(name).configure {
                    "showLine" to true
                    "thickness" to 4
                }.fillData(statPlotPoints)
        )
        return histogram;
    }
}