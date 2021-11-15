package inr.numass.scripts.tristan

import hep.dataforge.meta.Meta
import hep.dataforge.useValue
import inr.numass.data.analyzers.TimeAnalyzer
import inr.numass.data.api.NumassBlock
import inr.numass.data.api.NumassEvent

object TristanAnalyzer : TimeAnalyzer() {
    override fun getEvents(block: NumassBlock, meta: Meta): List<NumassEvent> {
        val t0 = getT0(block, meta)
        val summTime = meta.getInt("summTime", 200) //time limit in nanos for event summation
        var sequence = sequence {
            var last: NumassEvent? = null
            var amp = 0U
            getEventsWithDelay(block, meta).forEach { (event, time) ->
                when {
                    last == null -> {
                        last = event
                    }
                    time < 0 -> error("Can't be")
                    time < summTime -> {
                        //add to amplitude
                        amp += event.amplitude
                    }
                    time > t0 -> {
                        //accept new event and reset summator
                        if (amp != 0U) {
                            //construct new event with pileup
                            yield(NumassEvent(amp.toUShort(), last!!.timeOffset, last!!.owner))
                        } else {
                            //yield event without changes if there is no pileup
                            yield(last!!)
                        }
                        last = event
                        amp = event.amplitude.toUInt()
                    }
                    //else ignore event
                }
            }
        }

        meta.useValue("allowedChannels"){
            val list = it.list.map { it.int }
            sequence = sequence.filter { it.channel in list }
        }

        return  sequence.toList()
    }
}