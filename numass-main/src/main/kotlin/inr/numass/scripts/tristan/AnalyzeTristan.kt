package inr.numass.scripts.tristan

import hep.dataforge.context.Global
import hep.dataforge.fx.output.FXOutputManager
import hep.dataforge.plots.jfreechart.JFreeChartPlugin
import inr.numass.data.ProtoNumassPoint
import inr.numass.data.plotAmplitudeSpectrum
import inr.numass.data.transformChain
import kotlinx.coroutines.runBlocking
import java.io.File

fun main() {
    Global.output = FXOutputManager()
    JFreeChartPlugin().startGlobal()

    val file = File("D:\\Work\\Numass\\data\\2018_04\\Fill_3\\set_4\\p129(30s)(HV1=13000)").toPath()
    val point = ProtoNumassPoint.readFile(file)
    Global.plotAmplitudeSpectrum(point)

    point.blocks.firstOrNull { it.channel == 0 }?.let {
        Global.plotAmplitudeSpectrum(it, plotName = "0") {
            "title" to "pixel 0"
            "binning" to 50
        }
    }

    point.blocks.firstOrNull { it.channel == 4 }?.let {
        Global.plotAmplitudeSpectrum(it, plotName = "4") {
            "title" to "pixel 4"
            "binning" to 50
        }
        println("Number of events for pixel 4 is ${it.events.count()}")
    }

    runBlocking {
        listOf(0, 20, 50, 100, 200).forEach { window ->

            Global.plotAmplitudeSpectrum(point.transformChain { first, second ->
                val dt = second.timeOffset - first.timeOffset
                if (second.channel == 4 && first.channel == 0 && dt > window && dt < 1000) {
                    Pair((first.amplitude + second.amplitude).toShort(), second.timeOffset)
                } else {
                    null
                }
            }.also {
                println("Number of events for $window is ${it.events.count()}")
            }, plotName = "filtered.before.$window") {
                "binning" to 50
            }

        }

        listOf(0, 20, 50, 100, 200).forEach { window ->

            Global.plotAmplitudeSpectrum(point.transformChain { first, second ->
                val dt = second.timeOffset - first.timeOffset
                if (second.channel == 0 && first.channel == 4 && dt > window && dt < 1000) {
                    Pair((first.amplitude + second.amplitude).toShort(), second.timeOffset)
                } else {
                    null
                }
            }.also {
                println("Number of events for $window is ${it.events.count()}")
            }, plotName = "filtered.after.$window") {
                "binning" to 50
            }

        }

    }

    readLine()
}