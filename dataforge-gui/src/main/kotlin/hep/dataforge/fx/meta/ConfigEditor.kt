/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.fx.meta

import hep.dataforge.description.NodeDescriptor
import hep.dataforge.fx.dfIconView
import hep.dataforge.fx.values.ValueChooserFactory
import hep.dataforge.meta.Configuration
import javafx.scene.control.*
import javafx.scene.control.cell.TextFieldTreeTableCell
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.text.Text
import org.controlsfx.glyphfont.Glyph
import tornadofx.*

/**
 * FXML Controller class
 *
 * @author Alexander Nozik
 */
class ConfigEditor(val configuration: Configuration, title: String = "Configuration editor", val descriptor: NodeDescriptor? = null) : Fragment(title = title, icon = dfIconView) {

    val filter: (ConfigFX) -> Boolean = { cfg ->
        when (cfg) {
            is ConfigFXNode -> !(cfg.descriptor?.tags?.contains(NO_CONFIGURATOR_TAG) ?: false)
            is ConfigFXValue -> !(cfg.descriptor?.tags?.contains(NO_CONFIGURATOR_TAG) ?: false)
        }
    }

    override val root = borderpane {
        center = treetableview<ConfigFX> {
            root = ConfigTreeItem(ConfigFXRoot(configuration, descriptor))
            root.isExpanded = true
            sortMode = TreeSortMode.ALL_DESCENDANTS
            columnResizePolicy = TreeTableView.CONSTRAINED_RESIZE_POLICY
            column("Name") { param: TreeTableColumn.CellDataFeatures<ConfigFX, String> -> param.value.value.nameProperty }
                    .setCellFactory {
                        object : TextFieldTreeTableCell<ConfigFX, String>() {
                            override fun updateItem(item: String?, empty: Boolean) {
                                super.updateItem(item, empty)
                                contextMenu?.items?.removeIf { it.text == "Remove" }
                                if (!empty) {
                                    if (treeTableRow.item != null) {
                                        textFillProperty().bind(treeTableRow.item.hasValueProperty.objectBinding {
                                            if (it == true) {
                                                Color.BLACK
                                            } else {
                                                Color.GRAY
                                            }
                                        })
                                        if (treeTableRow.treeItem.value.hasValueProperty.get()) {
                                            contextmenu {
                                                item("Remove") {
                                                    action {
                                                        treeTableRow.item.remove()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

            column("Value") { param: TreeTableColumn.CellDataFeatures<ConfigFX, ConfigFX> ->
                param.value.valueProperty()
            }.setCellFactory {
                ValueCell()
            }

            column("Description") { param: TreeTableColumn.CellDataFeatures<ConfigFX, String> -> param.value.value.descriptionProperty }
                    .setCellFactory { param: TreeTableColumn<ConfigFX, String> ->
                        val cell = TreeTableCell<ConfigFX, String>()
                        val text = Text()
                        cell.graphic = text
                        cell.prefHeight = Control.USE_COMPUTED_SIZE
                        text.wrappingWidthProperty().bind(param.widthProperty())
                        text.textProperty().bind(cell.itemProperty())
                        cell
                    }
        }
    }

    private fun showNodeDialog(): String? {
        val dialog = TextInputDialog()
        dialog.title = "Node name selection"
        dialog.contentText = "Enter a name for new node: "
        dialog.headerText = null

        val result = dialog.showAndWait()
        return result.orElse(null)
    }

    private fun showValueDialog(): String? {
        val dialog = TextInputDialog()
        dialog.title = "Value name selection"
        dialog.contentText = "Enter a name for new value: "
        dialog.headerText = null

        val result = dialog.showAndWait()
        return result.orElse(null)
    }

    private inner class ValueCell : TreeTableCell<ConfigFX, ConfigFX?>() {

        public override fun updateItem(item: ConfigFX?, empty: Boolean) {
            if (!empty) {
                if (item != null) {
                    when (item) {
                        is ConfigFXValue -> {
                            text = null
                            val chooser = ValueChooserFactory.build(item.valueProperty, item.descriptor) {
                                item.value = it
                            }
                            graphic = chooser.node
                        }
                        is ConfigFXNode -> {
                            text = null
                            graphic = hbox {
                                button("node", Glyph("FontAwesome", "PLUS_CIRCLE")) {
                                    hgrow = Priority.ALWAYS
                                    maxWidth = Double.POSITIVE_INFINITY
                                    action {
                                        showNodeDialog()?.let {
                                            item.addNode(it)
                                        }
                                    }
                                }
                                button("value", Glyph("FontAwesome", "PLUS_SQUARE")) {
                                    hgrow = Priority.ALWAYS
                                    maxWidth = Double.POSITIVE_INFINITY
                                    action {
                                        showValueDialog()?.let {
                                            item.addValue(it)
                                        }
                                    }
                                }
                            }
                        }
                    }

                } else{
                    text = null
                    graphic = null
                }
            } else {
                text = null
                graphic = null
            }
        }

    }

    companion object {
        /**
         * The tag not to display node or value in configurator
         */
        const val NO_CONFIGURATOR_TAG = "nocfg"
    }
}
