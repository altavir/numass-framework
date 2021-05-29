package hep.dataforge.fx.table

import hep.dataforge.tables.TableFormat
import javafx.collections.FXCollections

class MutableTable(val tableFormat: TableFormat) : Iterable<MutableValues> {

    val rows = FXCollections.observableArrayList<MutableValues>();

    override fun iterator(): Iterator<MutableValues> {
        return rows.iterator()
    }
}