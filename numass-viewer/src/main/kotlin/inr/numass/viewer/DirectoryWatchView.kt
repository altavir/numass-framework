package inr.numass.viewer

import hep.dataforge.fx.dfIconView
import hep.dataforge.io.envelopes.Envelope
import inr.numass.data.NumassDataUtils
import inr.numass.data.NumassEnvelopeType
import inr.numass.data.storage.NumassDataLoader
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import javafx.scene.control.ContextMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tornadofx.*
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.WatchKey

class DirectoryWatchView : View(title = "Numass storage", icon = dfIconView) {

    val pathProperty = SimpleObjectProperty<Path>()
    private val dataController by inject<DataController>()

    private val ampView: AmplitudeView by inject()
    private val timeView: TimeView by inject()

    private var watcherProperty = pathProperty.objectBinding {
        it?.fileSystem?.newWatchService()
    }

    private val files = FXCollections.observableArrayList<DataController.CachedPoint>()

    private var watchJob: Job? = null

    init {
        dataController.points.addListener(MapChangeListener { change ->
            if (change.wasAdded()) {
                files.add(change.valueAdded)
            } else if (change.wasRemoved()) {
                files.remove(change.valueRemoved)
            }
        })

        watcherProperty.onChange { watchService ->
            watchJob?.cancel()
            if (watchService != null) {
                watchJob = app.context.launch(Dispatchers.IO) {
                    val key: WatchKey = pathProperty.get().register(watchService, ENTRY_CREATE)
                    coroutineContext[Job]?.invokeOnCompletion {
                        key.cancel()
                    }
                    while (isActive) {
                        try {
                            key.pollEvents().forEach { event ->
                                if (event.kind() == ENTRY_CREATE) {
                                    val path: Path = event.context() as Path
                                    if (path.fileName.toString().startsWith(NumassDataLoader.POINT_FRAGMENT_NAME)) {
                                        val envelope: Envelope = NumassEnvelopeType.infer(path)?.reader?.read(path)
                                            ?: kotlin.error("Can't read point file")
                                        val point = NumassDataUtils.read(envelope)
                                        files.add(dataController.getCachedPoint(path.toString(), point))
                                    }
                                }
                            }
                        } catch (x: Throwable) {
                            app.context.logger.error("Error during dynamic point read", x)
                        }
                    }
                }
            }
        }
    }


    override val root = splitpane {
        listview(files) {
            multiSelect(true)
            cellFormat { value: DataController.CachedPoint ->
                text = "${value.voltage}[${value.index}]"
                graphic = null
                contextMenu = ContextMenu().apply {
                    item("Info") {
                        action {
                            PointInfoView(value).openModal(escapeClosesWindow = true)
                        }
                    }
                }
            }
        }

        tabpane {
            tab("Amplitude spectra") {
                content = ampView.root
                isClosable = false
                //visibleWhen(ampView.isEmpty.not())
            }
            tab("Time spectra") {
                content = timeView.root
                isClosable = false
                //visibleWhen(ampView.isEmpty.not())
            }
        }
        setDividerPosition(0, 0.3);
    }
}
