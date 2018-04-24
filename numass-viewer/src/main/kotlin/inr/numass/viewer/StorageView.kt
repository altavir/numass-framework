package inr.numass.viewer

import hep.dataforge.fx.dfIconView
import hep.dataforge.fx.meta.MetaViewer
import hep.dataforge.fx.runGoal
import hep.dataforge.meta.Metoid
import hep.dataforge.storage.api.Loader
import hep.dataforge.storage.api.Storage
import hep.dataforge.storage.api.TableLoader
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import inr.numass.data.storage.NumassDataLoader
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.control.ContextMenu
import javafx.scene.control.TreeItem
import tornadofx.*

class StorageView(val storage: Storage) : View(title = "Numass storage", icon = dfIconView) {

    private val ampView: AmplitudeView by inject();
    private val spectrumView: SpectrumView by inject();
    private val hvView: HVView by inject();
    private val scView: SlowControlView by inject();

    private inner class Container(val id: String, val content: Any) {
        val checkedProperty = SimpleBooleanProperty(false)
        var checked by checkedProperty

        val infoView: UIComponent? by lazy {
            when (content) {
                is CachedPoint -> PointInfoView(content)
                is Metoid -> MetaViewer(content.meta, title = "Meta view: $id")
                else -> null
            }
        }

        init {
            checkedProperty.onChange { selected ->
                when (content) {
                    is CachedPoint -> {
                        if (selected) {
                            ampView.add(id, content)
                        } else {
                            ampView.remove(id)
                        }
                    }
                    is CachedSet -> {
                        if (selected) {
                            spectrumView.set(id, content)
                            hvView.add(id, content)
                        } else {
                            spectrumView.remove(id)
                            hvView.remove(id)
                        }
                    }
                    is TableLoader -> {
                        if (selected) {
                            scView.add(id, content)
                        } else {
                            scView.remove(id)
                        }
                    }
                }
            }
        }

        val children: List<Container>? by lazy {
            when (content) {
                is Storage -> (content.shelves().sorted() + content.loaders().sorted()).map { buildContainer(it, this) }
                is CachedSet -> content.points
                        .sortedBy { it.index }
                        .map { buildContainer(it, this) }
                        .toList()
                else -> null
            }
        }

        val hasChildren: Boolean
            get() = (content is Storage) || (content is NumassPoint)

    }


    override val root = splitpane {
        treeview<Container> {
            //isShowRoot = false
            root = TreeItem(Container(storage.name, storage))
            root.isExpanded = true
            runGoal("viewer.storage.populateTree") {
                populate { parent -> parent.value.children }
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
                    value.infoView?.let {
                        item("Info") {
                            action {
                                it.openModal(escapeClosesWindow = true)
                            }
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
                        content.path.toString()
                    } else {
                        content.name
                    }
                    Container(id, content as? CachedSet ?: CachedSet(content))
                }
                is NumassPoint -> {
                    Container("${parent.id}/${content.voltage}[${content.index}]", content as? CachedPoint ?: CachedPoint(content))
                }
                is Loader -> {
                    Container(content.path.toString(), content);
                }
                else -> throw IllegalArgumentException("Unknown content type: ${content::class.java}");
            }

}
