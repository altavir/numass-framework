package inr.numass.viewer

import hep.dataforge.fx.dfIconView
import hep.dataforge.fx.meta.MetaViewer
import hep.dataforge.meta.Meta
import hep.dataforge.meta.Metoid
import hep.dataforge.names.AlphanumComparator
import hep.dataforge.storage.Storage
import hep.dataforge.storage.files.FileTableLoader
import hep.dataforge.storage.tables.TableLoader
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import inr.numass.data.storage.NumassDataLoader
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.control.ContextMenu
import javafx.scene.control.TreeItem
import kotlinx.coroutines.runBlocking
import tornadofx.*

class StorageView(val storage: Storage) : View(title = "Numass storage", icon = dfIconView) {

    private val ampView: AmplitudeView by inject()
    private val timeView: TimeView by inject()
    private val spectrumView: SpectrumView by inject()
    private val hvView: HVView by inject()
    private val scView: SlowControlView by inject()

    init {
        ampView.clear()
        timeView.clear()
        spectrumView.clear()
        hvView.clear()
        scView.clear()
    }

    private inner class Container(val id: String, val content: Any) {
        val checkedProperty = SimpleBooleanProperty(false)
        var checked by checkedProperty

        val infoView: UIComponent by lazy {
            when (content) {
                is CachedPoint -> PointInfoView(content)
                is Metoid -> MetaViewer(content.meta, title = "Meta view: $id")
                else -> MetaViewer(Meta.empty(), title = "Meta view: $id")
            }
        }

        init {
            checkedProperty.onChange { selected ->
                when (content) {
                    is CachedPoint -> {
                        if (selected) {
                            ampView[id] = content
                            timeView[id] = content
                        } else {
                            ampView.remove(id)
                            timeView.remove(id)
                        }
                    }
                    is CachedSet -> {
                        if (selected) {
                            spectrumView[id] = content
                            hvView[id] = content
                        } else {
                            spectrumView.remove(id)
                            hvView.remove(id)
                        }
                    }
                    is TableLoader -> {
                        if (selected) {
                            scView[id] = content
                        } else {
                            scView.remove(id)
                        }
                    }
                }
            }
        }

        val children: List<Container>? by lazy {
            when (content) {
                is Storage -> runBlocking { content.children }.map { buildContainer(it, this) }.sortedWith(
                        object : Comparator<Container> {
                            private val alphanumComparator = AlphanumComparator()
                            override fun compare(o1: Container, o2: Container): Int = alphanumComparator.compare(o1.id, o2.id)
                        }
                )
                is NumassSet -> content.points
                        .sortedBy { it.index }
                        .map { buildContainer(it, this) }
                        .toList()
                else -> null
            }
        }

        val hasChildren: Boolean = (content is Storage) || (content is NumassSet)
    }


    override val root = splitpane {
        treeview<Container> {
            //isShowRoot = false
            root = TreeItem(Container(storage.name, storage))
            root.isExpanded = true
            lazyPopulate(leafCheck = { !it.value.hasChildren }) {
                it.value.children
            }
            cellFormat { value ->
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
                is Storage -> {
                    Container(content.fullName.toString(), content)
                }
                is NumassSet -> {
                    val id: String = if (content is NumassDataLoader) {
                        content.fullName.unescaped
                    } else {
                        content.name
                    }
                    Container(id, content as? CachedSet ?: CachedSet(content))
                }
                is NumassPoint -> {
                    Container("${parent.id}/${content.voltage}[${content.index}]", content as? CachedPoint
                            ?: CachedPoint(content))
                }
                is FileTableLoader -> {
                    Container(content.path.toString(), content);
                }
                else -> throw IllegalArgumentException("Unknown content type: ${content::class.java}");
            }
}
