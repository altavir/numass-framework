package inr.numass.viewer.test

import hep.dataforge.fx.plots.PlotContainer
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import hep.dataforge.tables.ValueMap
import tornadofx.*
import java.util.*

/**
 * Created by darksnake on 16-Apr-17.
 */
class JFCTest : View("My View") {
    val rnd = Random();

    val plot = JFreeChartFrame();
    val data = DataPlot("data");

    val button = button("test") {
        action {

            data.fillData(
                    (1..1000).map { ValueMap.of(arrayOf(XYAdapter.X_VALUE_KEY, XYAdapter.Y_VALUE_KEY), it, rnd.nextDouble()) }
            )
            plot.add(data)
        }
    };

    override val root = borderpane {
        center = PlotContainer(plot).root
        bottom {
            add(button)
        }
    }
}
