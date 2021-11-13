package inr.numass.data

import inr.numass.data.api.*
import org.apache.commons.collections4.queue.CircularFifoQueue
import org.apache.commons.math3.fitting.PolynomialCurveFitter
import org.apache.commons.math3.fitting.WeightedObservedPoint
import org.slf4j.LoggerFactory
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


class ChernovProcessor(
    val threshold: Short,
    val signalRange: IntRange,
    val tickSize: Int = 320,
    val signal: (Double) -> Double
) : SignalProcessor {

    private val fitter = PolynomialCurveFitter.create(2)

    private val signalMax = signal(0.0)

    /**
     * position an amplitude of peak relative to buffer end (negative)
     */
    private fun CircularFifoQueue<Short>.findMax(): Pair<Double, Double> {
        val data = this.mapIndexed { index, value ->
            WeightedObservedPoint(
                1.0,
                index.toDouble() - size + 1, // final point in zero
                value.toDouble()
            )
        }
        val (c, b, a) = fitter.fit(data)
        if (a > 0) error("Minimum!")
        val x = -b / 2 / a
        val y = -(b * b - 4 * a * c) / 4 / a
        return x to y
    }

    fun processBuffer(buffer: ShortBuffer): Sequence<OrphanNumassEvent> {

        val ringBuffer = CircularFifoQueue<Short>(5)

        fun roll() {
            ringBuffer.add(buffer.get())
        }

        return sequence<OrphanNumassEvent> {
            while (buffer.remaining() > 1) {
                roll()
                if (ringBuffer.isAtFullCapacity) {
                    if (ringBuffer.all { it > threshold && it <= ringBuffer[2] }) {
                        //Found bending, evaluating event
                        //TODO check end of frame
                        try {
                            val (pos, amp) = ringBuffer.findMax()

                            val timeInTicks = (pos + buffer.position() - 1)

                            val event = OrphanNumassEvent(amp.toInt().toShort(), (timeInTicks * tickSize).toLong())
                            yield(event)

                            //subtracting event from buffer copy
                            for (x in (signalRange.first + timeInTicks.toInt())..(signalRange.last + timeInTicks.toInt())) {
                                //TODO check all roundings
                                if (x >= 0 && x < buffer.limit()) {
                                    val oldValue = buffer.get(x)
                                    val newValue = oldValue - amp * signal(x - timeInTicks) / signalMax
                                    buffer.put(x, newValue.toInt().toShort())
                                }
                            }
                            println(buffer.array().joinToString())
                        } catch (ex: Exception) {
                            LoggerFactory.getLogger(javaClass).error("Something went wrong", ex)
                        }
                        roll()
                    }
                }
            }
        }
    }

    override fun process(parent: NumassBlock, frame: NumassFrame): Stream<NumassEvent> {
        val buffer = frame.signal.clone()
        return processBuffer(buffer).map { it.adopt(parent) }.asStream()
    }
}

