package inr.numass.viewer

import hep.dataforge.fx.dfIconView
import hep.dataforge.fx.except
import hep.dataforge.fx.runGoal
import hep.dataforge.fx.ui
import hep.dataforge.storage.Storage
import inr.numass.NumassProperties
import inr.numass.data.NumassDataUtils
import inr.numass.data.NumassFileEnvelope
import inr.numass.data.storage.NumassDataLoader
import inr.numass.data.storage.NumassDirectory
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Insets
import javafx.scene.control.Alert
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.controlsfx.control.StatusBar
import tornadofx.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class MainView : View(title = "Numass viewer", icon = dfIconView) {

    private val dataController by inject<DataController>()

    val storageView by inject<StorageView>()

    private val statusBar = StatusBar()
//    private val logFragment = LogFragment().apply {
//        addLogHandler(context.logger)
//    }

    private val pathProperty = SimpleObjectProperty<Path>()
    private var path: Path by pathProperty

    private val contentViewProperty = SimpleObjectProperty<UIComponent>()
    private var contentView: UIComponent? by contentViewProperty

    override val root = borderpane {
        prefHeight = 600.0
        prefWidth = 800.0
        top {
            toolbar {
                prefHeight = 40.0
                button("Load directory") {
                    action {
                        val chooser = DirectoryChooser()
                        chooser.title = "Select directory to view"
                        val homeDir = NumassProperties.getNumassProperty("numass.viewer.lastPath")
                        try {
                            if (homeDir == null) {
                                chooser.initialDirectory = File(".").absoluteFile
                            } else {
                                val file = File(homeDir)
                                if (file.isDirectory) {
                                    chooser.initialDirectory = file
                                } else {
                                    chooser.initialDirectory = file.parentFile
                                }
                            }

                            val rootDir = chooser.showDialog(primaryStage.scene.window)

                            if (rootDir != null) {
                                NumassProperties.setNumassProperty("numass.viewer.lastPath", rootDir.absolutePath)
                                app.context.launch {
                                    runLater {
                                        path = rootDir.toPath()
                                    }
                                    load(rootDir.toPath())
                                }
                            }
                        } catch (ex: Exception) {
                            NumassProperties.setNumassProperty("numass.viewer.lastPath", null)
                            error("Error", content = "Failed to laod file with message: ${ex.message}")
                        }
                    }
                }
                button("Load file") {
                    action {
                        val chooser = FileChooser()
                        chooser.title = "Select file to view"
                        val homeDir = NumassProperties.getNumassProperty("numass.viewer.lastPath")
                        try {
                            if (homeDir == null) {
                                chooser.initialDirectory = File(".").absoluteFile
                            } else {
                                chooser.initialDirectory = File(homeDir)
                            }


                            val file = chooser.showOpenDialog(primaryStage.scene.window)
                            if (file != null) {
                                NumassProperties.setNumassProperty("numass.viewer.lastPath",
                                    file.parentFile.absolutePath)
                                app.context.launch {
                                    runLater {
                                        path = file.toPath()
                                    }
                                    load(file.toPath())
                                }
                            }
                        } catch (ex: Exception) {
                            NumassProperties.setNumassProperty("numass.viewer.lastPath", null)
                            error("Error", content = "Failed to laod file with message: ${ex.message}")
                        }
                    }
                }

                label(pathProperty.stringBinding { it?.toString() ?: "NOT LOADED" }) {
                    padding = Insets(0.0, 0.0, 0.0, 10.0)
                    font = Font.font("System Bold", 13.0)
                }
                pane {
                    hgrow = Priority.ALWAYS
                }
//                togglebutton("Console") {
//                    isSelected = false
//                    logFragment.bindWindow(this@togglebutton)
//                }
            }
        }
        bottom = statusBar
    }

    init {
        contentViewProperty.onChange {
            root.center = it?.root
        }
    }

    private val spectrumView by inject<SpectrumView>()

    private suspend fun load(path: Path) {
        runLater {
            contentView = null
        }
        dataController.clear()
        if (Files.isDirectory(path)) {
            if (Files.exists(path.resolve(NumassDataLoader.META_FRAGMENT_NAME))) {
                //build set view
                runGoal(app.context, "viewer.load.set[$path]", Dispatchers.IO) {
                    title = "Load set ($path)"
                    message = "Building numass set..."
                    NumassDataLoader(app.context, null, path.fileName.toString(), path)
                } ui { loader: NumassDataLoader ->
                    contentView = spectrumView
                    dataController.addSet(loader.name, loader)

                } except {
                    alert(
                        type = Alert.AlertType.ERROR,
                        header = "Error during set loading",
                        content = it.toString()
                    ).show()
                }
            } else {
                //build storage
                app.context.launch {
                    val storageElement = NumassDirectory.INSTANCE.read(app.context, path) as Storage
                    withContext(Dispatchers.JavaFx) {
                        contentView = storageView
                        storageView.storageProperty.set(storageElement)
                    }
                }
            }
        } else {
            //Reading individual file
            val envelope = try {
                NumassFileEnvelope(path)
            } catch (ex: Exception) {
                runLater {
                    alert(
                        type = Alert.AlertType.ERROR,
                        header = "Can't load DF envelope from file $path",
                        content = ex.toString()
                    ).show()
                }
                null
            }

            envelope?.let {
                //try to read as point
                val point = NumassDataUtils.read(it)
                runLater {
                    contentView = AmplitudeView().apply {
                        dataController.addPoint(path.fileName.toString(), point)
                    }
                }
            }
        }
    }

}
