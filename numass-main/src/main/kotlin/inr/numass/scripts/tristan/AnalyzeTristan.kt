package inr.numass.scripts.tristan

import hep.dataforge.meta.Meta
import hep.dataforge.tables.Table
import hep.dataforge.values.Values
import inr.numass.data.analyzers.NumassAnalyzer
import inr.numass.data.api.NumassBlock
import inr.numass.data.api.NumassEvent
import inr.numass.data.api.NumassSet
import inr.numass.data.storage.ProtoNumassPoint
import java.util.stream.Stream

fun main(args: Array<String>) {
    val analyzer = object : NumassAnalyzer{
        override fun analyze(block: NumassBlock, config: Meta): Values {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun getEvents(block: NumassBlock, meta: Meta): Stream<NumassEvent> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun analyzeSet(set: NumassSet, config: Meta): Table {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }


    val file = ProtoNumassPoint.readFile("D:\\Work\\Numass\\data\\TRISTAN_11_2017\\df\\gun_16_19.df ")
    val events = Sequence { file.events.iterator() }.sortedBy { it.time }


}