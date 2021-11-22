package inr.numass.viewer

import hep.dataforge.fx.dfIconView
import hep.dataforge.io.envelopes.Envelope
import inr.numass.data.NumassDataUtils
import inr.numass.data.NumassEnvelopeType
import inr.numass.data.api.NumassPoint
import inr.numass.data.storage.NumassDataLoader
import javafx.scene.control.ContextMenu
import tornadofx.*
import java.nio.file.Path

class DirectoryWatchView : View(title = "Numass storage", icon = dfIconView) {

    private val dataController by inject<DataController>()

    private val ampView: AmplitudeView by inject()
    private val timeView: TimeView by inject()

//    private val files: ObservableList<DataController.CachedPoint> =
//        FXCollections.observableArrayList<DataController.CachedPoint>().apply {
//            bind(dataController.points) { _, v -> v }
//        }

    private fun readPointFile(path: Path): NumassPoint {
        val envelope: Envelope = NumassEnvelopeType.infer(path)?.reader?.read(path)
            ?: kotlin.error("Can't read point file")
        return NumassDataUtils.read(envelope)
    }

    override val root = splitpane {
        listview(dataController.files) {
            //multiSelect(true)
            cellFormat { path: Path ->
                text = path.fileName.toString()
                graphic = null
                if (path.fileName.toString().startsWith(NumassDataLoader.POINT_FRAGMENT_NAME)) {
                    val point = readPointFile(path)
                    val cachedPoint = dataController.addPoint(path.toString(), point)
                    //val point = dataController.getCachedPoint(value.toString())
                    contextMenu = ContextMenu().apply {
                        item("Info") {
                            action {
                                PointInfoView(cachedPoint).openModal(escapeClosesWindow = true)
                            }
                        }
                    }
                } else {
                    contextMenu = null
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
