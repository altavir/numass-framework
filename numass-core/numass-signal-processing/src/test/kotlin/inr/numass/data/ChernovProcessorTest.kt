package inr.numass.data

import org.apache.commons.math3.analysis.function.Gaussian
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ShortBuffer

class ChernovProcessorTest {
    val gaussian = Gaussian(1000.0, 0.0, 3.0)
    val processor = ChernovProcessor(10, -12..12, tickSize = 100) { gaussian.value(it) }

    val events = mapOf<Double, Double>(10.0 to 1.0, 16.0 to 0.5)

    val buffer = ShortArray(40) { i ->
        events.entries.sumOf { (pos, amp) -> amp * gaussian.value(pos - i.toDouble()) }.toInt().toShort()
    }

    @Test
    fun testPeaks() {
        println(buffer.joinToString())
        val peaks = processor.processBuffer(ShortBuffer.wrap(buffer)).toList()
        assertTrue(peaks.isNotEmpty())
        println(peaks.joinToString())
    }

}