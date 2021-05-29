package inr.numass.scripts.tristan

import hep.dataforge.context.Global
import hep.dataforge.fx.output.FXOutputManager
import hep.dataforge.plots.jfreechart.JFreeChartPlugin
import inr.numass.data.ProtoNumassPoint
import inr.numass.data.api.MetaBlock
import inr.numass.data.api.NumassBlock
import inr.numass.data.api.NumassPoint
import inr.numass.data.plotAmplitudeSpectrum
import java.io.File


private fun NumassPoint.getChannels(): Map<Int, NumassBlock> {
    return blocks.toList().groupBy { it.channel }.mapValues { entry ->
        if (entry.value.size == 1) {
            entry.value.first()
        } else {
            MetaBlock(entry.value)
        }
    }
}

fun main() {
    val file = File("D:\\Work\\Numass\\data\\2018_04\\Fill_3\\set_4\\p129(30s)(HV1=13000)").toPath()
    val point = ProtoNumassPoint.readFile(file)
    println(point.meta)

    Global.output = FXOutputManager()
    JFreeChartPlugin().startGlobal()

    point.getChannels().forEach{ (num, block) ->
        Global.plotAmplitudeSpectrum(numassBlock = block, plotName = num.toString())
    }

    readLine()
}