package inr.numass.scripts.tristan

import hep.dataforge.context.Global
import hep.dataforge.fx.output.FXOutputManager
import hep.dataforge.fx.plots.group
import hep.dataforge.plots.jfreechart.JFreeChartPlugin
import hep.dataforge.plots.output.plotFrame
import inr.numass.data.plotAmplitudeSpectrum
import inr.numass.data.storage.readNumassSet
import java.io.File

fun main() {
    Global.output = FXOutputManager()
    JFreeChartPlugin().startGlobal()

    val file = File("D:\\Work\\Numass\\data\\2018_04\\Fill_3\\set_36").toPath()
    val set = Global.readNumassSet(file)


    Global.plotFrame("compare") {
        listOf(12000.0, 13000.0, 14000.0, 14900.0).forEach {voltage->
            val point = set.optPoint(voltage).get()

            group("${set.name}/p${point.index}[${point.voltage}]") {
                plotAmplitudeSpectrum(point, "cut", analyzer = TristanAnalyzer) {
//                    "t0" to 3e3
                    "summTime" to 200
                    "sortEvents" to true
                    "inverted" to false
                }
                plotAmplitudeSpectrum(point, "uncut",analyzer = TristanAnalyzer){
                    "summTime" to 0
                    "sortEvents" to true
                    "inverted" to false
                }
            }
        }
    }

    readLine()
}