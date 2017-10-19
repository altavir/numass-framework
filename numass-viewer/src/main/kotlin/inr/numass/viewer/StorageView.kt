package inr.numass.viewer

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.exceptions.StorageException
import hep.dataforge.kodex.fx.dfIcon
import hep.dataforge.storage.filestorage.FileStorageFactory
import inr.numass.NumassProperties
import inr.numass.data.storage.NumassStorage
import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import javafx.stage.DirectoryChooser
import org.controlsfx.control.StatusBar
import tornadofx.*
import java.io.File
import java.net.URI

class StorageView : View(title = "Numass storage", icon = ImageView(dfIcon)) {

    val selected: ObservableList<Any> = FXCollections.observableArrayList();

    private val context: Context
        get() = Global.instance()

    val storageProperty = SimpleObjectProperty<NumassStorage>()
    var storage by storageProperty


    val storageNameProperty = SimpleStringProperty("")
    var storageName by storageNameProperty

    val statusBar = StatusBar();

    override val root = borderpane {
        top {
            toolbar {
                prefHeight = 40.0
                button("load") {
                    action {
                        val chooser = DirectoryChooser()
                        chooser.title = "Select numass storage root"
                        val storageRoot = NumassProperties.getNumassProperty("numass.storage.root")
                        try {
                            if (storageRoot == null) {
                                chooser.initialDirectory = File(".").absoluteFile
                            } else {
                                chooser.initialDirectory = File(storageRoot)
                            }
                        } catch (ex: Exception) {
                            NumassProperties.setNumassProperty("numass.storage.root", null)
                        }

                        val rootDir = chooser.showDialog(primaryStage.scene.window)

                        if (rootDir != null) {
                            NumassProperties.setNumassProperty("numass.storage.root", rootDir.absolutePath)
                            loadDirectory(rootDir.toURI())
                        }
                    }
                }
            }
            label(storageNameProperty) {
                padding = Insets(0.0, 0.0, 0.0, 10.0);
                font = Font.font("System Bold", 13.0);
            }
            pane {
                hgrow = Priority.ALWAYS
            }
            togglebutton("Console") {

            }
        }
        center {
            splitpane {
//                treetableview {
//
//                }
                tabpane {

                }
                setDividerPosition(0, 0.3);
            }
        }


        bottom = statusBar;

    }

    private fun loadDirectory(path: URI) {
        runAsync {
            updateTitle("Load storage ($path)")
            updateProgress(-1.0, -1.0);
            updateMessage("Building numass storage tree...")
            val root = NumassStorage(context, FileStorageFactory.buildStorageMeta(path, true, true));
            setRootStorage(root)
            Platform.runLater { storageName = "Storage: " + path }
            updateProgress(1.0, 1.0)
        }
    }

    fun setRootStorage(root: NumassStorage) {

        runAsync {
            updateTitle("Fill data to UI (" + root.name + ")")
            updateProgress(-1.0, 1.0)
            Platform.runLater { statusBar.progress = -1.0 }

            updateMessage("Loading numass storage tree...")

            try {
                storageProperty.set(root)
            } catch (ex: StorageException) {
                context.logger.error("Could not load the storage", ex);
            }

            //            callback.setProgress(1, 1);
            Platform.runLater { statusBar.progress = 0.0 }
            updateMessage("Numass storage tree loaded.")
            updateProgress(1.0, 1.0)
        }
    }
}
