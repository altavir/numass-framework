package inr.numass.scripts.analysis

import hep.dataforge.context.Global
import hep.dataforge.fx.output.FXOutputManager
import hep.dataforge.plots.jfreechart.JFreeChartPlugin
import inr.numass.data.ProtoNumassPoint
import java.io.File

fun main() {
    Global.output = FXOutputManager()
    JFreeChartPlugin().startGlobal()

    val file = File("D:\\Work\\Numass\\data\\test\\7.df").toPath()
    println(file)
    val point = ProtoNumassPoint.readFile(file)

    point.events.forEach {
        println("channel: ${it.owner.channel}, startTime: ${it.owner.startTime} timeOffset: ${it.timeOffset}\t amp: ${it.amplitude}")
    }

//    Global.plotFrame("compare") {
//        plotAmplitudeSpectrum(point, "cut") {
//            "t0" to 3e3
//            "sortEvents" to true
//        }
//        plotAmplitudeSpectrum(point, "uncut")
//    }
//
//    readLine()
}