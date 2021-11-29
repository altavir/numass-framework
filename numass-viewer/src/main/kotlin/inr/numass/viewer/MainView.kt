package inr.numass.viewer

import hep.dataforge.asName
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

    private val pathProperty = SimpleObjectProperty<Path?>()

    private val contentViewProperty = SimpleObjectProperty<UIComponent>()
    private var contentView: UIComponent? by contentViewProperty
    private val spectrumView by inject<SpectrumView>()
    private val amplitudeView by inject<AmplitudeView>()
    private val directoryWatchView by inject<DirectoryWatchView>()

    init {
        contentViewProperty.onChange {
            root.center = it?.root
        }
    }

    private fun loadDirectory(path: Path){
        app.context.launch {
            dataController.clear()
            runLater {
                pathProperty.set(path)
                contentView = null
            }
            if (Files.exists(path.resolve(NumassDataLoader.META_FRAGMENT_NAME))) {
                //build set view
                runGoal(app.context, "viewer.load.set[$path]") {
                    title = "Load set ($path)"
                    message = "Building numass set..."
                    NumassDataLoader(app.context, null, path.fileName.toString(), path)
                } ui { loader: NumassDataLoader ->
                    contentView = spectrumView
                    dataController.addSet(loader.name.asName(), loader)

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
                    val storageElement =
                        NumassDirectory.INSTANCE.read(app.context, path) as? Storage
                    withContext(Dispatchers.JavaFx) {
                        contentView = storageView
                        storageView.storageProperty.set(storageElement)
                    }
                }
            }
        }
    }

    private fun loadFile(path: Path){
        app.context.launch {
            dataController.clear()
            runLater {
                pathProperty.set(path)
                contentView = null
            }
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
                    contentView = amplitudeView
                    dataController.addPoint(path.fileName.toString().asName(), point)
                }
            }
        }
    }

    private fun watchDirectory(path: Path){
        app.context.launch {
            dataController.clear()
            runLater {
                pathProperty.set(path)
                contentView = directoryWatchView
                dataController.watchPathProperty.set(path)
            }
        }
    }


    override val root = borderpane {
        prefHeight = 600.0
        prefWidth = 800.0
        top {
            //bypass top configuration bar and only watch directory
            app.parameters.named["directory"]?.let{ pathString ->
                val path = Path.of(pathString).toAbsolutePath()
                watchDirectory(path)
                toolbar{
                    prefHeight = 40.0
                    label("Watching $path") {
                        padding = Insets(0.0, 0.0, 0.0, 10.0)
                        font = Font.font("System Bold", 13.0)
                    }
                    pane {
                        hgrow = Priority.ALWAYS
                    }
                }
                return@top
            }

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
                                loadDirectory(rootDir.toPath())
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
                                loadFile(file.toPath())
                            }
                        } catch (ex: Exception) {
                            NumassProperties.setNumassProperty("numass.viewer.lastPath", null)
                            error("Error", content = "Failed to laod file with message: ${ex.message}")
                        }
                    }
                }

                button("Watch directory") {
                    action {
                        val chooser = DirectoryChooser()
                        chooser.title = "Select directory to watch"
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

                            val dir = chooser.showDialog(primaryStage.scene.window)

                            if (dir != null) {
                                NumassProperties.setNumassProperty("numass.viewer.lastPath", dir.absolutePath)
                                watchDirectory(dir.toPath())
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

}
