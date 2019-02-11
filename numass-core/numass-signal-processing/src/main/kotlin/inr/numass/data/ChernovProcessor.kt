package inr.numass.data

import hep.dataforge.meta.Meta
import inr.numass.data.api.NumassBlock
import inr.numass.data.api.NumassEvent
import inr.numass.data.api.NumassFrame
import inr.numass.data.api.SignalProcessor
import org.apache.commons.collections4.queue.CircularFifoQueue
import java.nio.ShortBuffer
import java.util.stream.Stream
import kotlin.streams.asStream


private fun ShortBuffer.clone(): ShortBuffer {
    val clone = ShortBuffer.allocate(capacity())
    rewind()//copy from the beginning
    clone.put(this)
    rewind()
    clone.flip()
    return clone
}


class ChernovProcessor(val meta: Meta) : SignalProcessor {
    val threshold = meta.getValue("threshold").number.toShort()
    val signalRange: IntRange = TODO()
    val signal: (Double) -> Double = { TODO() }
    val tickSize: Int = TODO()

    private fun CircularFifoQueue<Short>.findMax(): Pair<Double, Double> {
        TODO()
    }

    override fun analyze(parent: NumassBlock, frame: NumassFrame): Stream<NumassEvent> {
        return sequence<NumassEvent> {
            val events = HashMap<Double, Double>()
            val buffer = frame.signal.clone()

            val ringBuffer = CircularFifoQueue<Short>(5)
            while (buffer.remaining() > 0) {
                ringBuffer.add(buffer.get())
                val lastValue = ringBuffer[1] ?: -1
                val currentValue = ringBuffer[0]
                if (lastValue > threshold && currentValue < lastValue) {
                    //Found bending, evaluating event

                    ringBuffer.add(buffer.get())//do another step to have 5-points
                    //TODO check end of frame
                    val (pos, amp) = ringBuffer.findMax()
                    val event = NumassEvent(amp.toShort(), pos.toLong() * tickSize, parent)
                    yield(event)

                    //subtracting event from buffer copy
                    for (x in signalRange) {
                        //TODO check all roundings
                        val position = buffer.position() - x.toShort()
                        val oldValue = buffer.get(position)
                        val newValue = oldValue - amp * signal(x.toDouble())
                        buffer.put(position, newValue.toShort())
                    }
                }
            }
        }.asStream()
    }
}

