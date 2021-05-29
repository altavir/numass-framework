package hep.dataforge.plots.demo

/**
 * Created by darksnake on 29-Apr-17.
 */

import hep.dataforge.plots.jfreechart.JFreeChartFrame
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import tornadofx.*

class JFreeChartTest : App() {

    override fun start(stage: Stage) {
        val root = BorderPane()
        val node1 = JFreeChartFrame().fxNode
        //val node2 = ChartViewer(JFreeChart("plot", XYPlot(null, NumberAxis(), NumberAxis(), XYLineAndShapeRenderer())))
        root.center = JFreeChartFrame().fxNode
        val scene = Scene(root, 800.0, 600.0)
        stage.title = "JFC test"
        stage.scene = scene
        stage.show()
    }
}

fun main(args: Array<String>) {
    Application.launch(JFreeChartTest::class.java, *args)
}
