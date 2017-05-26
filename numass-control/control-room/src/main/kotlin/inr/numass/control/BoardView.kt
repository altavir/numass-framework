package inr.numass.control

import hep.dataforge.control.devices.Device
import hep.dataforge.fx.fragments.FXFragment
import hep.dataforge.fx.fragments.FragmentWindow
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
                hbox {
                    alignment = Pos.CENTER
                    vgrow = Priority.ALWAYS;
                    prefHeight = 40.0
                    text("Server") // TODO add fancy style here
                    separator(Orientation.VERTICAL)
                    var serverLabel: Hyperlink by singleAssign();
                    togglebutton("Start") {
                        isSelected = false
                        disableProperty().bind(controller.serverManagerProperty.isNull)
                        action {
                            if (isSelected) {
                                text = "Stop"
                                controller.serverManager.startServer()
                                serverLabel.text = controller.serverManager.link;
                            } else {
                                text = "Start"
                                controller.serverManager.stopServer()
                                serverLabel.text = ""
                            }
                        }
                    }
                    indicator {
                        bind(controller.serverManager.isStarted)
                    }
                    serverLabel = hyperlink {
                        action {
                            hostServices.showDocument(controller.serverManager.link);
                        }
                    }
                }
                separator(Orientation.HORIZONTAL)
                hbox {
                    alignment = Pos.CENTER
                    vgrow = Priority.ALWAYS;
                    prefHeight = 40.0
                    text("Storage")
                    separator(Orientation.VERTICAL)
                    label(stringBinding(controller.storage) {
                        controller.storage.fullPath
                    })
                }
                separator(Orientation.HORIZONTAL)
                vbox {
                    vgrow = Priority.ALWAYS;
                    prefHeight = 40.0
                    bindChildren(controller.devices) { connection ->
                        hbox {
                            alignment = Pos.CENTER
                            vgrow = Priority.ALWAYS;
                            text("Device: " + connection.device.name)
                            separator(Orientation.VERTICAL)
                            indicator {
                                bind(connection, Device.INITIALIZED_STATE)
                            }
                            val viewButton = togglebutton("View")
                            FragmentWindow(FXFragment.buildFromNode(connection.device.name) { connection.fxNode }).bindTo(viewButton)
                        }
                    }
                }
            }
        }
    }
}
