package inr.numass.scripts.tristan

import inr.numass.data.channel
import inr.numass.data.plotAmplitudeSpectrum
import inr.numass.data.storage.ProtoNumassPoint
import inr.numass.data.transformChain
import java.io.File

fun main(args: Array<String>) {
    val file = File("D:\\Work\\Numass\\data\\TRISTAN_11_2017\\df\\gun_16_19.df").toPath()
    val point = ProtoNumassPoint.readFile(file)
    point.plotAmplitudeSpectrum()

    point.blocks.firstOrNull { it.channel == 0 }?.let {
        it.plotAmplitudeSpectrum(plotName = "0") {
            "title" to "pixel 0"
            "binning" to 50
        }
    }

    point.blocks.firstOrNull { it.channel == 4 }?.let {
        it.plotAmplitudeSpectrum(plotName = "4") {
            "title" to "pixel 4"
            "binning" to 50
        }
        println("Number of events for pixel 4 is ${it.events.count()}")
    }

    runBlocking {
        listOf(0, 20, 50, 100, 200).forEach { window ->

            point.transformChain { first, second ->
                val dt = second.timeOffset - first.timeOffset
                if (second.channel == 4 && first.channel == 0 && dt > window && dt < 1000) {
                    Pair((first.amplitude + second.amplitude).toShort(), second.timeOffset)
                } else {
                    null
                }
            }.also {
                println("Number of events for $window is ${it.events.count()}")
            }.plotAmplitudeSpectrum(plotName = "filtered.before.$window") {
                "binning" to 50
            }

        }

        listOf(0, 20, 50, 100, 200).forEach { window ->

            point.transformChain { first, second ->
                val dt = second.timeOffset - first.timeOffset
                if (second.channel == 0 && first.channel == 4 && dt > window && dt < 1000) {
                    Pair((first.amplitude + second.amplitude).toShort(), second.timeOffset)
                } else {
                    null
                }
            }.also {
                println("Number of events for $window is ${it.events.count()}")
            }.plotAmplitudeSpectrum(plotName = "filtered.after.$window") {
                "binning" to 50
            }

        }

    }
}