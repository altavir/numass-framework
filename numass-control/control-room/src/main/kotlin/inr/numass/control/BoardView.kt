package inr.numass.control

import hep.dataforge.fx.dfIcon
import hep.dataforge.storage.filestorage.FileStorage
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import tornadofx.*

/**
 * Created by darksnake on 11-May-17.
 */
class BoardView : View("Numass control board", ImageView(dfIcon)) {
    private val controller: BoardController by inject();

    override val root = borderpane {
        prefHeight = 200.0
        prefWidth = 200.0
        center {
            vbox {
                //Server pane
                titledpane(title = "Server", collapsible = false) {
                    vgrow = Priority.ALWAYS;
                    hbox {
                        alignment = Pos.CENTER_LEFT
                        prefHeight = 40.0
                        togglebutton("Start") {
                            isSelected = false
                            disableProperty().bind(controller.serverManagerProperty.booleanBinding { it == null })
                            action {
                                if (isSelected) {
                                    text = "Stop"
                                    controller.serverManagerProperty.value?.startServer()
                                } else {
                                    text = "Start"
                                    controller.serverManagerProperty.value?.stopServer()
                                }
                            }
                        }
                        text("Started: ") {
                            paddingHorizontal = 5
                        }
                        indicator {
                            bind(controller.serverManagerProperty.select { it?.isStartedProperty ?: false.toProperty() })
                        }
                        separator(Orientation.VERTICAL)
                        text("Address: ")
                        hyperlink {
                            textProperty().bind(controller.serverManagerProperty.stringBinding {
                                it?.link ?: ""
                            })
                            action {
                                hostServices.showDocument(controller.serverManagerProperty.value?.link);
                            }
                        }
                    }
                }
                titledpane(title = "Storage", collapsible = true) {
                    vgrow = Priority.ALWAYS;
                    hbox {
                        alignment = Pos.CENTER_LEFT
                        prefHeight = 40.0
                        label(controller.storageProperty.stringBinding { storage ->
                            if (storage == null) {
                                "Storage not initialized"
                            } else {
                                if (storage is FileStorage) {
                                    "Path: " + storage.dataDir;
                                } else {
                                    "Name: " + storage.fullName
                                }
                            }
                        })
                    }
                }
                separator(Orientation.HORIZONTAL)
                scrollpane(fitToWidth = true, fitToHeight = true) {
                    vgrow = Priority.ALWAYS;
                    vbox {
                        prefHeight = 40.0
                        bindChildren(controller.devices) { connection ->
                            titledpane(
                                    title = "Device: " + connection.device.name,
                                    collapsible = true,
                                    node = connection.getBoardView()
                            )
                        }
                    }
                }
            }
        }
    }
}
