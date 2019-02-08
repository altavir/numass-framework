package inr.numass.scripts

import hep.dataforge.buildContext
import hep.dataforge.meta.buildMeta
import hep.dataforge.nullable
import hep.dataforge.plots.PlotGroup
import hep.dataforge.plots.data.DataPlot
import inr.numass.NumassPlugin
import inr.numass.data.analyzers.NumassAnalyzer
import inr.numass.data.analyzers.SmartAnalyzer
import inr.numass.data.analyzers.withBinning
import inr.numass.data.api.NumassSet
import inr.numass.data.storage.NumassDirectory
import inr.numass.displayChart

/**
 * Investigating slices of single point for differences at the beginning and end
 */
fun main(args: Array<String>) {
    val context = buildContext("NUMASS", NumassPlugin::class.java) {
        rootDir = "D:\\Work\\Numass\\sterile\\2017_05"
        dataDir = "D:\\Work\\Numass\\data\\2017_05_frames"
    }
    //val rootDir = File("D:\\Work\\Numass\\data\\2017_05\\Fill_2")

    val storage = NumassDirectory.read(context, "Fill_3") ?: error("Storage not found")


    val analyzer = SmartAnalyzer()

    val meta = buildMeta {
        "t0" to 15e3
//        "window.lo" to 400
//        "window.up" to 1600
    }

    val set = storage.provide("set_4", NumassSet::class.java).nullable ?: error("Set does not exist")

    val frame = displayChart("slices").apply {
        plots.setType<DataPlot>()
        plots.configureValue("showLine", true)
    }

    listOf(10, 58, 103).forEach { index ->
        val group = PlotGroup("point_$index")
        group.setType<DataPlot>()
        val point = set.find { it.index == index } ?: error("Point not found")


//        val blockSizes = point.meta.getValue("events").list.map { it.int }
//        val startTimes = point.meta.getValue("start_time").list.map { it.time }

        group.add(DataPlot.plot("spectrum", analyzer.getAmplitudeSpectrum(point, meta).withBinning(32), NumassAnalyzer.AMPLITUDE_ADAPTER))

//        runBlocking {
//            val events = point.events.toList()
//            var startIndex = 0
//            val blocks = blockSizes.zip(startTimes).map { (size, startTime) ->
//                SimpleBlock.produce(startTime, Duration.ofSeconds(5)) {
//                    events.subList(startIndex, startIndex + size)
//                }.also { startIndex += size }
//            }
//
//            blocks.forEachIndexed { index, block ->
//                group.add(DataPlot.plot("block_$index", analyzer.getAmplitudeSpectrum(block).withBinning(20), NumassAnalyzer.AMPLITUDE_ADAPTER) {
//                    "visible" to false
//                })
//            }
//        }
        frame.add(group)
    }
}