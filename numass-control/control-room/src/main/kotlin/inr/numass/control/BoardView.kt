package inr.numass.control

import hep.dataforge.control.devices.Device
import hep.dataforge.control.devices.PortSensor
import hep.dataforge.control.measurements.Sensor
import hep.dataforge.fx.fragments.FXFragment
import hep.dataforge.fx.fragments.FragmentWindow
import hep.dataforge.storage.filestorage.FileStorage
import inr.numass.control.NumassControlUtils.getDFIcon
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.control.Hyperlink
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import tornadofx.*

/**
 * Created by darksnake on 11-May-17.
 */
class BoardView : View("Numass control board", ImageView(getDFIcon())) {
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
                        var serverLabel: Hyperlink by singleAssign();
                        togglebutton("Start") {
                            isSelected = false
                            disableProperty().bind(controller.serverManagerProperty.isNull)
                            action {
                                if (isSelected) {
                                    text = "Stop"
                                    controller.serverManager?.startServer()
                                    serverLabel.text = controller.serverManager?.link;
                                } else {
                                    text = "Start"
                                    controller.serverManager?.stopServer()
                                    serverLabel.text = ""
                                }
                            }
                        }
                        text("Started: ") {
                            paddingHorizontal = 5
                        }
                        indicator {
                            bind(controller.serverManagerProperty.select { it.isStarted })
                        }
                        separator(Orientation.VERTICAL)
                        text("Address: ")
                        serverLabel = hyperlink {
                            action {
                                hostServices.showDocument(controller.serverManager?.link);
                            }
                        }
                    }
                }
                titledpane(title = "Storage", collapsible = true) {
                    vgrow = Priority.ALWAYS;
                    hbox {
                        alignment = Pos.CENTER_LEFT
                        prefHeight = 40.0
                        label(stringBinding(controller.storageProperty) {
                            val storage = controller.storage
                            if (storage == null) {
                                "Storage not initialized"
                            } else {
                                if (storage is FileStorage) {
                                    "Path: " + storage.dataDir;
                                } else {
                                    "Name: " + storage.fullPath
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
                            titledpane(title = "Device: " + connection.device.name, collapsible = true) {
                                hbox {
                                    alignment = Pos.CENTER_LEFT
                                    vgrow = Priority.ALWAYS;
                                    deviceStateIndicator(connection, Device.INITIALIZED_STATE)
                                    deviceStateIndicator(connection, PortSensor.CONNECTED_STATE)
                                    deviceStateIndicator(connection, Sensor.MEASURING_STATE)
                                    deviceStateIndicator(connection, "storing")
                                    pane {
                                        hgrow = Priority.ALWAYS
                                    }
                                    togglebutton("View") {
                                        isSelected = false
                                        FragmentWindow(FXFragment.buildFromNode(connection.device.name) { connection.fxNode }).bindTo(this)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
