package hep.dataforge.meta

import hep.dataforge.tables.ColumnTable
import hep.dataforge.tables.ListTable
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class MetaMorphTest {

    private fun reconstruct(obj: Any): Any {
        val baos = ByteArrayOutputStream()
        val oos = ObjectOutputStream(baos)
        oos.writeObject(obj)
        val ois = ObjectInputStream(ByteArrayInputStream(baos.toByteArray()))
        return ois.readObject()
    }

    @Test
    fun tableListTable() {
        val table = ListTable.Builder("a", "b", "c").apply {
            row(1, 2, 3)
            row(4, 5, 6)
        }.build()


        assertEquals(table, reconstruct(table))
    }

    @Test
    fun tableColumnTable() {
        val table = ColumnTable.copy(ListTable.Builder("a", "b", "c").apply {
            row(1, 2, 3)
            row(4, 5, 6)
        }.build())


        assertEquals(table, reconstruct(table))
    }

}