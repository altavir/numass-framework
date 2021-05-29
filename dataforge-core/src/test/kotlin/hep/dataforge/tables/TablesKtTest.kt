package hep.dataforge.tables

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.floor

class TablesKtTest {
    val table = buildTable {
        (1..99).forEach {
            row(
                    "a" to (it.toDouble() / 100.0),
                    "b" to it,
                    "c" to (it.toDouble() / 2.0)
            )
        }
    }

    @Test
    fun groupTest() {
        val res = table.groupBy { floor(it["a"].double / 0.1) }
        assertEquals(10, res.size)
    }

    @Test
    fun testReduction() {
        val reduced = table.sumByStep("a", 0.1)
        assertEquals(10, reduced.size())
        assertEquals(55, reduced.first()["b"].int)
    }
}