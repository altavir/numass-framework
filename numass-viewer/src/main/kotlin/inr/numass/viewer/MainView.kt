package inr.numass.viewer

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.fx.*
import hep.dataforge.fx.fragments.LogFragment
import hep.dataforge.storage.commons.StorageManager
import inr.numass.NumassProperties
import inr.numass.data.api.NumassPoint
import inr.numass.data.legacy.NumassFileEnvelope
import inr.numass.data.storage.NumassDataLoader
import inr.numass.data.storage.NumassStorageFactory
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Insets
import javafx.scene.control.Alert
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import kotlinx.coroutines.experimental.async
import org.controlsfx.control.StatusBar
import tornadofx.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class MainView(val context: Context = Global.getContext("viewer")) : View(title = "Numass viewer", icon = dfIconView) {

    private val statusBar = StatusBar();
    private val logFragment = LogFragment().apply {
        addLogHandler(context.logger)
    }

    private val pathProperty = SimpleObjectProperty<Path>()
    private var path: Path by pathProperty

    val contentViewProperty = SimpleObjectProperty<UIComponent>()
    var contentView: UIComponent? by contentViewProperty


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
                                chooser.initialDirectory = File(homeDir)
                            }
                        } catch (ex: Exception) {
                            NumassProperties.setNumassProperty("numass.viewer.lastPath", null)
                        }

                        val rootDir = chooser.showDialog(primaryStage.scene.window)

                        if (rootDir != null) {
                            NumassProperties.setNumassProperty("numass.viewer.lastPath", rootDir.absolutePath)
                            async {
                                runLater {
                                    path = rootDir.toPath()
                                }
                                load(rootDir.toPath())
                            }
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
                        } catch (ex: Exception) {
                            NumassProperties.setNumassProperty("numass.viewer.lastPath", null)
                        }

                        val file = chooser.showOpenDialog(primaryStage.scene.window)
                        if (file != null) {
                            NumassProperties.setNumassProperty("numass.viewer.lastPath", file.parentFile.absolutePath)
                            async {
                                runLater {
                                    path = file.toPath()
                                }
                                load(file.toPath())
                            }
                        }
                    }
                }

                label(pathProperty.asString()) {
                    padding = Insets(0.0, 0.0, 0.0, 10.0);
                    font = Font.font("System Bold", 13.0);
                }
                pane {
                    hgrow = Priority.ALWAYS
                }
                togglebutton("Console") {
                    isSelected = false
                    logFragment.bindWindow(this@togglebutton)
                }
            }
        }
        bottom = statusBar
    }

    init {
        contentViewProperty.onChange {
            root.center = it?.root
        }
    }

    private suspend fun load(path: Path) {
        runLater {
            contentView = null
        }
        if (Files.isDirectory(path)) {
            if (Files.exists(path.resolve(NumassDataLoader.META_FRAGMENT_NAME))) {
                //build set view
                runGoal("viewer.load.set[$path]") {
                    title = "Load set ($path)"
                    message = "Building numass set..."
                    NumassDataLoader.fromDir(context, path)
                } ui {
                    contentView = SpectrumView().apply {
                        add(it.name, it)
                    }
                } except {
                    alert(
                            type = Alert.AlertType.ERROR,
                            header = "Error during set loading",
                            content = it.toString()
                    ).show()
                }
            } else {
                //build storage
                runGoal("viewer.load.storage[$path]") {
                    title = "Load storage ($path)"
                    message = "Building numass storage tree..."
                    StorageManager.buildStorage(
                            context,
                            NumassStorageFactory.buildStorageMeta(path.toUri(), true, false)
                    )
                } ui {
                    contentView = StorageView(it)
                } except {
                    alert(
                            type = Alert.AlertType.ERROR,
                            header = "Error during storage loading",
                            content = it.toString()
                    ).show()
                }
            }
        } else {
            //Reading individual file
            val envelope = try {
                NumassFileEnvelope.open(path,true)
            } catch (ex: Exception){
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
                if(it.meta.hasMeta("external_meta")){
                    //try to read as point
                    val point = NumassPoint.read(it)
                    runLater {
                        contentView = AmplitudeView().apply {
                            add(path.toString(), point)
                        }
                    }
                } else {
                    alert(
                            type = Alert.AlertType.ERROR,
                            header = "Unknown envelope content: $path"
                    ).show()
                }
            }
        }
    }

}
