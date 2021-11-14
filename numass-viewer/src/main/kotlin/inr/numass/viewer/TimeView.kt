package inr.numass.viewer

import hep.dataforge.configure
import hep.dataforge.fx.dfIcon
import hep.dataforge.fx.except
import hep.dataforge.fx.plots.PlotContainer
import hep.dataforge.fx.runGoal
import hep.dataforge.fx.ui
import hep.dataforge.goals.Goal
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import hep.dataforge.plots.Plottable
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import hep.dataforge.tables.Adapters
import hep.dataforge.values.ValueMap
import inr.numass.data.analyzers.TimeAnalyzer
import inr.numass.data.api.NumassPoint
import javafx.beans.Observable
import javafx.beans.binding.DoubleBinding
import javafx.collections.FXCollections
import javafx.collections.ObservableMap
import javafx.scene.image.ImageView
import tornadofx.*

class TimeView : View(title = "Numass time spectrum plot", icon = ImageView(dfIcon)) {

    private val frame = JFreeChartFrame().configure {
        "title" to "Time plot"
        node("xAxis") {
            "title" to "delay"
            "units" to "us"

        }
        node("yAxis") {
            "title" to "number of events"
            "type" to "log"
        }
    }.apply {
        plots.configure {
            "connectionType" to "step"
            "thickness" to 2
            "showLine" to true
            "showSymbol" to false
            "showErrors" to false
        }.setType<DataPlot>()
    }

//    val stepProperty = SimpleDoubleProperty()
//    var step by stepProperty
//
//    private val container = PlotContainer(frame).apply {
//        val binningSelector: ChoiceBox<Int> = ChoiceBox(FXCollections.observableArrayList(1, 5, 10, 20, 50)).apply {
//            minWidth = 0.0
//            selectionModel.selectLast()
//            stepProperty.bind(this.selectionModel.selectedItemProperty())
//        }
//        addToSideBar(0, binningSelector)
//    }

    private val container = PlotContainer(frame)

    private val data: ObservableMap<String, NumassPoint> = FXCollections.observableHashMap()
    private val plots: ObservableMap<String, Goal<Plottable>> = FXCollections.observableHashMap()

    val isEmpty = booleanBinding(data) { isEmpty() }

    private val progress = object : DoubleBinding() {
        init {
            bind(plots)
        }

        override fun computeValue(): Double {
            return plots.values.count { it.isDone }.toDouble() / data.size;
        }

    }

    init {
        data.addListener { _: Observable ->
            invalidate()
        }
    }

    override val root = borderpane {
        center = container.root
    }

    /**
     * Put or replace current plot with name `key`
     */
    operator fun set(key: String, point: NumassPoint) {
        data[key] = point
    }

    fun addAll(data: Map<String, NumassPoint>) {
        this.data.putAll(data);
    }

    private val analyzer = TimeAnalyzer();


    private fun invalidate() {
        data.forEach { key, point ->
            plots.getOrPut(key) {
                runGoal<Plottable>("loadAmplitudeSpectrum_$key") {

                    val initialEstimate = analyzer.analyze(point)
                    val cr = initialEstimate.getDouble("cr")

                    val binNum = 200//inputMeta.getInt("binNum", 1000);
                    val binSize = 1.0 / cr * 10 / binNum * 1e6//inputMeta.getDouble("binSize", 1.0 / cr * 10 / binNum * 1e6)

                    val histogram = analyzer.getEventsWithDelay(point, Meta.empty())
                            .map { it.second.toDouble() / 1000.0 }
                            .groupBy { Math.floor(it / binSize) }
                            .toSortedMap()
                            .map {
                                ValueMap.ofPairs("x" to it.key, "count" to it.value.count())
                            }

                    DataPlot(key, adapter = Adapters.buildXYAdapter("x", "count"))
                            .configure {
                                "showLine" to true
                                "showSymbol" to false
                                "showErrors" to false
                                "connectionType" to "step"
                            }.fillData(histogram)

                } ui { plot ->
                    frame.add(plot)
                    progress.invalidate()
                } except {
                    progress.invalidate()
                }
            }
            plots.keys.filter { !data.containsKey(it) }.forEach { remove(it) }
        }
    }

    fun clear() {
        data.clear()
        plots.values.forEach {
            it.cancel()
        }
        plots.clear()
        invalidate()
    }

    /**
     * Remove the plot and cancel loading task if it is in progress.
     */
    fun remove(name: String) {
        frame.plots.remove(Name.ofSingle(name))
        plots[name]?.cancel()
        plots.remove(name)
        data.remove(name)
        progress.invalidate()
    }

    /**
     * Set frame content to the given map. All keys not in the map are removed.
     */
    fun setAll(map: Map<String, NumassPoint>) {
        plots.clear();
        //Remove obsolete keys
        data.keys.filter { !map.containsKey(it) }.forEach {
            remove(it)
        }
        this.addAll(map);
    }

}

