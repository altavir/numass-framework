package hep.dataforge.fx.output

import hep.dataforge.context.Context
import hep.dataforge.fx.FXPlugin
import hep.dataforge.fx.dfIcon
import javafx.geometry.Side
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import tornadofx.*

/**
 * An interface to produce border panes for content.
 */
interface FXDisplay {
    fun display(stage: String, name: String, action: BorderPane.() -> Unit)
}

fun buildDisplay(context: Context): FXDisplay {
    return TabbedFXDisplay().also {
        context.load(FXPlugin::class.java).display(it)
    }
}

class TabbedFXDisplay : View("DataForge display", ImageView(dfIcon)), FXDisplay {

    private val stages: MutableMap<String, TabbedStage> = HashMap();

    private val stagePane = TabPane().apply {
        side = Side.LEFT
    }
    override val root = borderpane {
        center = stagePane
    }

    override fun display(stage: String, name: String, action: BorderPane.() -> Unit) {
        runLater {
            stages.getOrPut(stage) {
                TabbedStage(stage).apply {
                    val stageFragment = this
                    stagePane.tab(stage) {
                        content = stageFragment.root
                        isClosable = false
                    }
                }
            }.apply {
                action.invoke(getTab(name).pane)
            }
        }
    }


    inner class TabbedStage(val stage: String) : Fragment(stage) {
        private var tabs: MutableMap<String, DisplayTab> = HashMap()
        val tabPane = TabPane()

        override val root = borderpane {
            center = tabPane
        }

        fun getTab(tabName: String): DisplayTab {
            return tabs.getOrPut(tabName) { DisplayTab(tabName) }
        }


        inner class DisplayTab(val name: String) {
            private val tab: Tab = Tab(name)
            val pane: BorderPane = BorderPane()

            init {
                tab.content = pane
                tabPane.tabs.add(tab)
            }
        }
    }

}
