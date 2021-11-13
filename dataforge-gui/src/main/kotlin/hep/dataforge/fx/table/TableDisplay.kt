package hep.dataforge.fx.table

import hep.dataforge.fx.dfIconView
import hep.dataforge.tables.Table
import hep.dataforge.values.Value
import hep.dataforge.values.ValueType
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import org.controlsfx.control.spreadsheet.*
import tornadofx.*

/**
 * Table display fragment
 */
class TableDisplay(title: String? = null) : Fragment(title = title, icon = dfIconView) {

    val tableProperty = SimpleObjectProperty<Table>()
    var table: Table? by tableProperty


    private fun buildCell(row: Int, column: Int, value: Value): SpreadsheetCell {
        return when (value.type) {
            ValueType.NUMBER -> SpreadsheetCellType.DOUBLE.createCell(row, column, 1, 1, value.double)
            else -> SpreadsheetCellType.STRING.createCell(row, column, 1, 1, value.string)
        }
    }

    private val spreadsheet = CustomSpreadSheetView().apply {
        isEditable = false
//        isShowColumnHeader = false
    }

    override val root = borderpane {
        top = toolbar {
            button("Export as text") {
                action(::export)
            }
        }
        center = spreadsheet;
    }

    init {
        tableProperty.onChange {
            runLater {
                spreadsheet.grid = buildGrid(it)
            }
        }
    }

    private fun buildGrid(table: Table?): Grid? {
        return table?.let {
            GridBase(table.size(), table.format.count()).apply {
                val format = table.format;

                columnHeaders.setAll(format.names.asList())
//        rows += format.names.asList().observable();

                (0 until table.size()).forEach { i ->
                    rows += (0 until format.count())
                        .map { j -> buildCell(i, j, table.get(format.names[j], i)) }
                        .asObservable()
                }
            }
        }
    }

    private fun export() {
        table?.let { table ->
            chooseFile("Save table data to...", emptyArray(), mode = FileChooserMode.Save).firstOrNull()?.let {
                //            if(!it.exists()){
//                it.createNewFile()
//            }

                it.printWriter().use { writer ->
                    writer.println(table.format.names.joinToString(separator = "\t"))
                    table.forEach { values ->
                        writer.println(table.format.names.map { values[it] }.joinToString(separator = "\t"))
                    }
                    writer.flush()
                }
            }
        }
    }

    class CustomSpreadSheetView : SpreadsheetView() {
        override fun copyClipboard() {
            val posList = selectionModel.selectedCells

            val columns = posList.map { it.column }.distinct().sorted()
            val rows = posList.map { it.row }.distinct().sorted()


            //building text
            val text = rows.joinToString(separator = "\n") { row ->
                columns.joinToString(separator = "\t") { column ->
                    grid.rows[row][column].text
                }
            }

            //TODO add HTML binding

            val content = ClipboardContent()
            content.putString(text);
//            content.put(DataFormat("SpreadsheetView"), list)
            Clipboard.getSystemClipboard().setContent(content)
        }
        //TODO add pasteClipboard
    }
}
