package inr.numass.viewer

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.exceptions.StorageException
import hep.dataforge.kodex.fx.dfIcon
import hep.dataforge.kodex.fx.runGoal
import hep.dataforge.storage.api.Loader
import hep.dataforge.storage.api.Storage
import hep.dataforge.storage.filestorage.FileStorageFactory
import inr.numass.NumassProperties
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import inr.numass.data.storage.NumassDataLoader
import inr.numass.data.storage.NumassStorage
import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Insets
import javafx.scene.control.TreeItem
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import javafx.stage.DirectoryChooser
import org.controlsfx.control.StatusBar
import tornadofx.*
import java.io.File
import java.net.URI
import kotlin.streams.toList

class StorageView(private val context: Context = Global.instance()) : View(title = "Numass storage", icon = ImageView(dfIcon)) {


    val storageProperty = SimpleObjectProperty<Storage>()
    var storage by storageProperty


    private val storageNameProperty = SimpleStringProperty("")
    private var storageName by storageNameProperty

    private val statusBar = StatusBar();

    private val ampView: AmplitudeView by inject();
    private val spectrumView: SpectrumView by inject();
    private val hvView: HVView by inject();
    private val scView: SlowControlView by inject();

    private data class NamedPoint(val id: String, val point: NumassPoint)

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

        }
        center {
            splitpane {
                treeview<Any> {
                    storageProperty.onChange {
                        root = TreeItem(it)
                        populate { parent ->
                            val value = parent.value
                            when (value) {
                                is Storage -> value.shelves() + value.loaders()
                                is NumassSet -> value.points.map { point -> NamedPoint("${getSetName(value)}/${point.voltage}", point) }.toList()
                                else -> null
                            }
                        }
                    }
                    cellFormat { value ->
                        when (value) {
                            is Storage -> text = value.name
                            is NumassSet -> {
                                text = null
                                graphic = checkbox {
                                    text = value.name
                                    val setName = getSetName(value)
                                    selectedProperty().onChange { selected ->
                                        if (selected) {
                                            spectrumView.add(setName, value)
                                            hvView.add(value)
                                        } else {
                                            spectrumView.remove(setName)
                                            hvView.remove(value)
                                        }
                                    }
                                }
                            }
                            is NamedPoint -> {
                                text = null
                                graphic = checkbox {
                                    text = value.id
                                    selectedProperty().onChange { selected ->
                                        if (selected) {
                                            ampView.add(value.id, value.point)
                                        } else {
                                            ampView.remove(id)
                                        }
                                    }
                                }
                            }
                            else -> {
                                text = (value as Loader).name
                            }
                        }
                    }
                }
                tabpane {
                    tab("Amplitude spectra", ampView.root) {
                        isClosable = false
                        visibleWhen(ampView.isEmpty.not())
                    }
                    tab("HV", hvView.root) {
                        isClosable = false
                        visibleWhen(hvView.isEmpty.not())
                    }
                    tab("Numass spectra", spectrumView.root) {
                        isClosable = false
                        visibleWhen(spectrumView.isEmpty.not())
                    }
                }
                setDividerPosition(0, 0.3);
            }
        }


        bottom = statusBar;

    }

    private fun getSetName(value: NumassSet): String {
        return if (value is NumassDataLoader) {
            value.path
        } else {
            value.name
        }
    }

    private fun loadDirectory(path: URI) {
        runGoal("loadDirectory[$path]") {
            title = "Load storage ($path)"
            progress = -1.0
            message = "Building numass storage tree..."
            val root = NumassStorage(context, FileStorageFactory.buildStorageMeta(path, true, true));
            setRootStorage(root)
            Platform.runLater { storageName = "Storage: " + path }
            progress = 1.0
        }
    }

    fun setRootStorage(root: Storage) {
        runGoal("loadStorage[${root.name}]") {
            title = "Fill data to UI (" + root.name + ")"
            progress = -1.0
            runLater { statusBar.progress = -1.0 }

            message = "Loading numass storage tree..."

            runLater {
                try {
                    storageProperty.set(root)
                } catch (ex: StorageException) {
                    context.logger.error("Could not load the storage", ex);
                }
            }

            //            callback.setProgress(1, 1);
            runLater { statusBar.progress = 0.0 }
            message = "Numass storage tree loaded."
            progress = 1.0
        }
    }
}
