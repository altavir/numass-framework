package inr.numass.viewer

import hep.dataforge.asName
import hep.dataforge.fx.dfIconView
import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.names.AlphanumComparator
import hep.dataforge.names.Name
import inr.numass.data.NumassDataUtils
import inr.numass.data.NumassEnvelopeType
import inr.numass.data.api.NumassPoint
import inr.numass.data.storage.NumassDataLoader
import kotlinx.coroutines.launch
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

    //private class PointContainer(val path: Path, val checkedProperty: BooleanProperty = SimpleBooleanProperty())

    override val root = splitpane {
        listview(dataController.files.sorted { l, r -> AlphanumComparator.compare(l.toString(), r.toString()) }) {
            cellFormat { path: Path ->
                if (path.fileName.toString().startsWith(NumassDataLoader.POINT_FRAGMENT_NAME)) {
                    val name = Name.of(path.map { it.toString().asName() })
                    text = null
                    graphic = checkbox(path.fileName.toString()).apply {
                        isSelected = dataController.points.containsKey(name)
                        selectedProperty().onChange {
                            if (it) {
                                app.context.launch {
                                    dataController.addPoint(name, readPointFile(path))
                                }
                            } else {
                                dataController.remove(name)
                            }
                        }
                    }

//                    app.context.launch {
//                        val point = readPointFile(path)
//                        val cachedPoint = dataController.addPoint(path.toString().asName(), point)
//
//                        //val point = dataController.getCachedPoint(value.toString())
//                        withContext(Dispatchers.JavaFx) {
//                            contextMenu = ContextMenu().apply {
//                                item("Info") {
//                                    action {
//                                        PointInfoView(cachedPoint).openModal(escapeClosesWindow = true)
//                                    }
//                                }
//                            }
//                        }
//                    }

                } else {
                    text = path.fileName.toString()
                    graphic = null
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
