package hep.dataforge.maths.expressions

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sqrt


class DSFieldTest {

    @Test
    fun testNormal() {
        val x = 0
        val context = DSField(1, "amp", "pos", "sigma")

        val gauss: DSNumber = with(context) {
            val amp = variable("amp", 1)
            val pos = variable("pos", 0)
            val sigma = variable("sigma", 1)
            amp / (sigma * sqrt(2 * PI)) * exp(-pow(pos - x, 2) / pow(sigma, 2) / 2)
        }

        //println(gauss)
        assertEquals(1.0 / sqrt(2.0 * PI), gauss.toDouble(), 0.001)
        assertEquals(0.0, gauss.deriv("pos"), 0.001)
    }

//    @Test
//    fun performanceTest(){
//        (1..100000000).forEach{
//            testNormal()
//        }
//    }


}