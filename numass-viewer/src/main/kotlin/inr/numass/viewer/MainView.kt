package inr.numass.viewer

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.exceptions.StorageException
import hep.dataforge.fx.fragments.FragmentWindow
import hep.dataforge.fx.fragments.LogFragment
import hep.dataforge.fx.work.Work
import hep.dataforge.fx.work.WorkManager
import hep.dataforge.fx.work.WorkManagerFragment
import hep.dataforge.meta.Annotated
import hep.dataforge.names.Named
import hep.dataforge.storage.api.Storage
import inr.numass.NumassProperties
import inr.numass.data.NumassData
import inr.numass.storage.NumassStorage
import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.control.TreeTableView.CONSTRAINED_RESIZE_POLICY
import javafx.scene.input.MouseEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.stage.DirectoryChooser
import javafx.util.Pair
import org.controlsfx.control.StatusBar
import tornadofx.*
import java.io.File
import java.util.logging.Level

/**
 * Created by darksnake on 14-Apr-17.
 */
class MainView : View() {
    override val root: AnchorPane by fxml("/fxml/MainView.fxml");

    private val numassLoaderView: NumassLoaderView by inject()

    private val consoleButton: ToggleButton by fxid()
    private val processManagerButton: ToggleButton by fxid()
    private val loadDirectoryButton: Button by fxid()
    private val loadRemoteButton: Button by fxid()
    private val storagePathLabel: Label by fxid()

    private val loaderPane: BorderPane by fxid()
    private val treePane: BorderPane  by fxid()
    private val statusBar: StatusBar  by fxid()

    private val storageProperty = SimpleObjectProperty<Storage>();

    init {
        loadDirectoryButton.action {
            val chooser = DirectoryChooser()
            chooser.title = "Select numass storage root"
            val storageRoot = NumassProperties.getNumassProperty("numass.storage.root")
            if (storageRoot == null) {
                chooser.initialDirectory = File(".").absoluteFile
            } else {
                chooser.initialDirectory = File(storageRoot)
            }

            val rootDir = chooser.showDialog(primaryStage.scene.window)

            if (rootDir != null) {
                NumassProperties.setNumassProperty("numass.storage.root", rootDir.absolutePath)
                loadDirectory(rootDir.toURI().toString())
            }
        }
        loadRemoteButton.action { onLoadRemote() }

        treePane.center {
            treetableview<Item> {
                column("name", Item::getName)
                column("time", Item::getTime)

                addEventHandler(MouseEvent.MOUSE_CLICKED) { e: MouseEvent ->
                    if (e.clickCount == 2) {
                        val value = focusModel.focusedCell.treeItem.value
                        if (value.content is NumassData) {
                            numassLoaderView.loadData(value.content)
                            loaderPane.center = numassLoaderView.root
                        }
                    }
                }

                columnResizePolicy = CONSTRAINED_RESIZE_POLICY

                storageProperty.addListener { _, _, value ->
                    if (value != null) {
                        Platform.runLater {
                            root = TreeItem(Item(value));

                            populate { parent ->
                                val storage = parent.value.content;
                                if (storage is Storage) {
                                    //TODO add legacy loaders here?
                                    storage.shelves().map(::Item) + storage.loaders().map(::Item)
                                } else {
                                    null
                                }
                            }
                        }
                    } else {
                        // TODO clear
                    }
                }

            }
        }

        val logFragment = LogFragment()
        logFragment.addRootLogHandler()
        //logFragment.hookStd();
        FragmentWindow(logFragment).bindTo(consoleButton)
        FragmentWindow(WorkManagerFragment(getWorkManager())).bindTo(processManagerButton)
    }

    private fun loadDirectory(path: String) {
        getWorkManager().startWork("viewer.loadDirectory") { work: Work ->
            work.title = "Load storage ($path)"
            work.progress = -1.0
            work.status = "Building numass storage tree..."
            try {
                val root = NumassStorage.buildNumassRoot(path, true, false)
                setRootStorage(root)
                Platform.runLater { storagePathLabel.text = "Storage: " + path }
            } catch (ex: Exception) {
                work.progress = 0.0
                work.status = "Failed to load storage " + path
                log.log(Level.SEVERE, null, ex)
            }
        }
    }

    private val context: Context
        get() = Global.instance()

    @Synchronized private fun getWorkManager(): WorkManager {
        return Global.instance().getFeature(WorkManager::class.java);
    }

    fun setRootStorage(root: NumassStorage) {

        getWorkManager().cleanup()
        getWorkManager().startWork("viewer.storage.load") { callback: Work ->
            callback.title = "Fill data to UI (" + root.name + ")"
            callback.progress = -1.0
            Platform.runLater { statusBar.progress = -1.0 }

            callback.status = "Loading numass storage tree..."

            try {
                storageProperty.set(root)
            } catch (ex: StorageException) {
                log.log(Level.SEVERE, null, ex)
            }

            //            callback.setProgress(1, 1);
            Platform.runLater { statusBar.progress = 0.0 }
            callback.status = "Numass storage tree loaded."
            callback.setProgressToMax()
        }
    }

    private fun onLoadRemote() {
        // Create the custom dialog.
        val dialog = Dialog<Pair<String, String>>()
        dialog.title = "Remote storage selection"
        dialog.headerText = "Select remote storage login options and run"

        val loginButtonType = ButtonType("Load", ButtonBar.ButtonData.OK_DONE)
        dialog.dialogPane.buttonTypes.addAll(loginButtonType, ButtonType.CANCEL)

        // Create the username and password labels and fields.
        val grid = GridPane()
        grid.hgap = 10.0
        grid.vgap = 10.0
        grid.padding = Insets(20.0, 150.0, 10.0, 10.0)

        val storageText = TextField()
        storageText.prefWidth = 350.0
        storageText.text = "sftp://trdat:Anomaly@192.168.111.1"
        val runText = TextField()
        runText.promptText = "Run name"

        grid.add(Label("Storage path:"), 0, 0)
        grid.add(storageText, 1, 0)
        grid.add(Label("Run name:"), 0, 1)
        grid.add(runText, 1, 1)

        dialog.dialogPane.content = grid

        // Request focus on the username field by default.
        storageText.requestFocus()

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter { dialogButton ->
            if (dialogButton == loginButtonType) {
                Pair(storageText.text, runText.text)
            } else {
                null;
            }
        }

        val result = dialog.showAndWait()

        if (result.isPresent) {
            val path = result.get().key + "/data/" + result.get().value
            loadDirectory(path)
        }
    }

    class Item(val content: Named) {
        fun getName(): String {
            return content.name;
        }

        fun getTime(): String {
            if (content is NumassData) {
                if(content.startTime() == null){
                    return ""
                } else {
                    return content.startTime().toString()
                }
            } else if (content is Annotated) {
                return content.meta().getString("file.timeModified", "")
            } else {
                return "";
            }
        }

    }
}