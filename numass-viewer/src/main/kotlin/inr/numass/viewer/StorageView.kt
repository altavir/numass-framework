package inr.numass.viewer

import hep.dataforge.fx.dfIconView
import hep.dataforge.fx.meta.MetaViewer
import hep.dataforge.meta.Meta
import hep.dataforge.meta.Metoid
import hep.dataforge.names.AlphanumComparator
import hep.dataforge.storage.Storage
import hep.dataforge.storage.files.FileTableLoader
import hep.dataforge.storage.tables.TableLoader
import inr.numass.data.NumassDataUtils
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import inr.numass.data.storage.EnvelopeStorageElement
import inr.numass.data.storage.NumassDataLoader
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import javafx.scene.control.ContextMenu
import javafx.scene.control.TreeItem
import kotlinx.coroutines.Job
import tornadofx.*


class StorageView : View(title = "Numass storage", icon = dfIconView) {

    val storageProperty = SimpleObjectProperty<Storage>()
    val storage by storageProperty

    private val dataController by inject<DataController>()

    private val ampView: AmplitudeView by inject()
    private val timeView: TimeView by inject()
    private val spectrumView: SpectrumView by inject()
    private val hvView: HVView by inject()
    private val scView: SlowControlView by inject()

    private inner class Container(val id: String, val content: Any) {
        val checkedProperty = SimpleBooleanProperty(false)
        var checked by checkedProperty

        val infoView: UIComponent by lazy {
            when (content) {
                is NumassPoint -> PointInfoView(dataController.getCachedPoint(id, content))
                is Metoid -> MetaViewer(content.meta, title = "Meta view: $id")
                else -> MetaViewer(Meta.empty(), title = "Meta view: $id")
            }
        }

        //val watchedProperty = SimpleBooleanProperty(false)

        init {
            checkedProperty.onChange { selected ->
                when (content) {
                    is NumassPoint -> {
                        if (selected) {
                            dataController.addPoint(id, content)
                        } else {
                            dataController.remove(id)
                        }
                    }
                    is NumassSet -> {
                        if (selected) {
                            dataController.addSet(id, content)
                        } else {
                            dataController.remove(id)
                        }
                    }
                    is TableLoader -> {
                        if (selected) {
                            dataController.addSc(id, content)
                        } else {
                            dataController.remove(id)
                        }
                    }
                }
            }

//            watchedProperty.onChange {
//                toggleWatch(it)
//            }
        }

        val children: ObservableList<Container>? by lazy {
            when (content) {
                is Storage -> content.getChildren().mapNotNull {
                    if (it is EnvelopeStorageElement) {
                        it.envelope?.let { envelope ->
                            try {
                                buildContainer(NumassDataUtils.read(envelope), this)
                            } catch (ex: Exception) {
                                null
                            }
                        }
                    } else {
                        buildContainer(it, this)
                    }
                }.sortedWith(Comparator.comparing({ it.id }, AlphanumComparator)).asObservable()
                is NumassSet -> content.points
                    .sortedBy { it.index }
                    .map { buildContainer(it, this) }
                    .toObservable()
                else -> null
            }
        }

        val hasChildren: Boolean = (content is Storage) || (content is NumassSet)

        private var watchJob: Job? = null

    }


    override val root = splitpane {
        treeview<Container> {
            //isShowRoot = false
            storageProperty.onChange { storage ->
                dataController.clear()
                if (storage == null) return@onChange
                root = TreeItem(Container(storage.name, storage))
                root.isExpanded = true
                lazyPopulate(leafCheck = {
                    !it.value.hasChildren
                }) {
                    it.value.children
                }

            }

            cellFormat { value: Container ->
                when (value.content) {
                    is Storage -> {
                        text = value.content.name
                        graphic = null
                    }
                    is NumassSet -> {
                        text = null
                        graphic = checkbox(value.content.name).apply {
                            selectedProperty().bindBidirectional(value.checkedProperty)
                        }
                    }
                    is NumassPoint -> {
                        text = null
                        graphic = checkbox("${value.content.voltage}[${value.content.index}]").apply {
                            selectedProperty().bindBidirectional(value.checkedProperty)
                        }
                    }
                    is TableLoader -> {
                        text = null
                        graphic = checkbox(value.content.name).apply {
                            selectedProperty().bindBidirectional(value.checkedProperty)
                        }
                    }
                    else -> {
                        text = value.id
                        graphic = null
                    }
                }
                contextMenu = ContextMenu().apply {
                    item("Clear all") {
                        action {
                            this@cellFormat.treeItem.uncheckAll()
                        }
                    }
                    item("Info") {
                        action {
                            value.infoView.openModal(escapeClosesWindow = true)
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
            tab("HV") {
                content = hvView.root
                isClosable = false
                //visibleWhen(hvView.isEmpty.not())
            }
            tab("Numass spectra") {
                content = spectrumView.root
                isClosable = false
                //visibleWhen(spectrumView.isEmpty.not())
            }
            tab("Slow control") {
                content = scView.root
                isClosable = false
                //visibleWhen(scView.isEmpty.not())
            }
        }
        setDividerPosition(0, 0.3);
    }


    private fun TreeItem<Container>.uncheckAll() {
        this.value.checked = false
        this.children.forEach { it.uncheckAll() }
    }


    private fun buildContainer(content: Any, parent: Container): Container =
        when (content) {
            is Storage -> Container(content.fullName.toString(), content)
            is NumassSet -> {
                val id: String = if (content is NumassDataLoader) {
                    content.fullName.unescaped
                } else {
                    content.name
                }
                Container(id, content)
            }
            is NumassPoint -> {
                Container("${parent.id}/${content.voltage}[${content.index}]", content)
            }
            is FileTableLoader -> Container(content.path.toString(), content)
            else -> throw IllegalArgumentException("Unknown content type: ${content::class.java}");
        }
}
