package inr.numass.control

import hep.dataforge.context.Context
import hep.dataforge.exceptions.StorageException
import hep.dataforge.meta.Meta
import hep.dataforge.server.ServerManager
import hep.dataforge.storage.commons.StorageFactory
import inr.numass.client.ClientUtils
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventHandler
import javafx.scene.control.Hyperlink
import tornadofx.*

/**
 * Created by darksnake on 18-May-17.
 */
class ServerView() : View("Numass server controller") {
    val contextProperty = SimpleObjectProperty<Context>()
    var context by contextProperty

    val serverManagerProperty = SimpleObjectProperty<ServerManager>()
    var serverManager: ServerManager by serverManagerProperty


    var label: Hyperlink by singleAssign();
    override val root = borderpane {
        center {
            hbox {
                togglebutton("Server") {
                    isSelected = false
                    disableProperty().bind(serverManagerProperty.isNull)
                    action {
                        if (isSelected) {
                            serverManager.startServer()
                            label.text = serverManager.link;
                        } else {
                            serverManager.stopServer()
                            label.text = ""
                        }
                    }
                }
                label = hyperlink{
                    action {
                        hostServices.showDocument(serverManager.link);
                    }
                }
            }
        }
    }

    init {
        NumassControlUtils.setDFStageIcon(primaryStage)
        contextProperty.addListener { _, oldValue, newValue ->
            if (oldValue != newValue) {
                if (newValue != null) {
                    serverManager = newValue.pluginManager().getOrLoad(ServerManager::class.java);
                } else {
                    serverManagerProperty.set(null);
                }
            }
        }
        primaryStage.onCloseRequest = EventHandler { serverManager.stopServer() }
    }

    fun configure(meta: Meta) {
        meta.optMeta("storage").ifPresent { node ->
            context.logger.info("Creating storage for server with meta {}", node)
            //building storage in a separate thread
            runAsync {
                val numassRun = ClientUtils.getRunName(meta)
                var storage = StorageFactory.buildStorage(context, node)
                if (!numassRun.isEmpty()) {
                    try {
                        storage = storage.buildShelf(numassRun, Meta.empty())
                    } catch (e: StorageException) {
                        context.logger.error("Failed to build shelf", e)
                    }

                }
                serverManager.addStorage("numass", storage);
            }
        }
    }

}
