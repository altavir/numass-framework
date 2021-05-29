/*
 * Copyright  2018 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package hep.dataforge.fx.output

import hep.dataforge.context.BasicPlugin
import hep.dataforge.context.Context
import hep.dataforge.context.PluginDef
import hep.dataforge.context.PluginTag
import hep.dataforge.fx.FXPlugin
import hep.dataforge.fx.dfIconView
import hep.dataforge.io.OutputManager
import hep.dataforge.io.OutputManager.Companion.OUTPUT_STAGE_KEY
import hep.dataforge.io.output.Output
import hep.dataforge.meta.Meta
import hep.dataforge.plots.PlotFactory
import hep.dataforge.plots.Plottable
import hep.dataforge.tables.Table
import javafx.beans.binding.ListBinding
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.geometry.Side
import javafx.scene.control.Tab
import javafx.scene.layout.BorderPane
import tornadofx.*

/**
 * Provide a map which is synchronized on UI thread
 */
private fun <T, R> ObservableMap<T, R>.ui(): ObservableMap<T, R> {
    val res = FXCollections.observableHashMap<T, R>()
    this.addListener { change: MapChangeListener.Change<out T, out R> ->
        runLater {
            if (change.wasRemoved()) {
                res.remove(change.key)
            }
            if (change.wasAdded()) {
                res[change.key] = change.valueAdded
            }
        }
    }
    return res
}

class OutputContainer(val context: Context, val meta: Meta) :
    Fragment(title = "[${context.name}] DataForge output container", icon = dfIconView) {

    private val stages: ObservableMap<String, OutputStageContainer> = FXCollections.observableHashMap()

    private val uiStages = stages.ui()

    override val root = tabpane {
        //tabs for each stage
        side = Side.LEFT
        tabs.bind(uiStages) { key, value ->
            Tab(key).apply {
                content = value.root
                isClosable = false
            }
        }
    }

    private fun buildStageContainer(): OutputStageContainer {
        return if (meta.getBoolean("treeStage", false)) {
            TreeStageContainer()
        } else {
            TabbedStageContainer()
        }
    }

    fun get(meta: Meta): Output {
        synchronized(this) {
            val stage = meta.getString(OUTPUT_STAGE_KEY, "@default")
            val container = stages.getOrPut(stage) { buildStageContainer() }
            return container.get(meta)
        }
    }

    /**
     * Create a new output
     */
    private fun buildOutput(type: String, meta: Meta): FXOutput {
        return when {
            type.startsWith(Plottable.PLOTTABLE_TYPE) -> if (context.get(PlotFactory::class.java) != null) {
                FXPlotOutput(context, meta)
            } else {
                context.logger.error("Plot output not defined in the context")
                FXTextOutput(context)
            }
            type.startsWith(Table.TABLE_TYPE) -> FXTableOutput(context)
            else -> FXWebOutput(context)
        }
    }

    private abstract inner class OutputStageContainer : Fragment() {
        val outputs: ObservableMap<String, FXOutput> = FXCollections.observableHashMap()

        fun get(meta: Meta): FXOutput {
            synchronized(outputs) {
                val name = meta.getString(OutputManager.OUTPUT_NAME_KEY)
                val type = meta.getString(OutputManager.OUTPUT_TYPE_KEY, Output.TEXT_TYPE)
                return outputs.getOrPut(name) { buildOutput(type, meta) }
            }
        }
    }

    private inner class TreeStageContainer : OutputStageContainer() {
        override val root = borderpane {
            left {
                // name list
                //TODO replace by tree
                listview<String> {
                    items = object : ListBinding<String>() {
                        init {
                            bind(outputs)
                        }

                        override fun computeValue(): ObservableList<String> {
                            return outputs.keys.toList().observable()
                        }
                    }
                    onUserSelect {
                        this@borderpane.center = outputs[it]!!.view.root
                    }
                }
            }
        }
    }

    private inner class TabbedStageContainer : OutputStageContainer() {

        private val uiOutputs = outputs.ui()

        override val root = tabpane {
            //tabs for each output
            side = Side.TOP
            tabs.bind(uiOutputs) { key, value ->
                Tab(key).apply {
                    content = value.view.root
                    isClosable = false
                }
            }
        }
    }
}

@PluginDef(
    name = "output.fx",
    dependsOn = ["hep.dataforge.fx", "hep.dataforge.plots"],
    info = "JavaFX based output manager"
)
class FXOutputManager(
    meta: Meta = Meta.empty(),
    viewConsumer: Context.(OutputContainer) -> Unit = { getOrLoad(FXPlugin::class.java).display(it) }
) : OutputManager, BasicPlugin(meta) {

    override val tag = PluginTag(name = "output.fx", dependsOn = *arrayOf("hep.dataforge:fx"))

    override fun attach(context: Context) {
        super.attach(context)
        //Check if FX toolkit is started
        context.load<FXPlugin>()
    }

    private val container: OutputContainer by lazy {
        OutputContainer(context, meta).also { viewConsumer.invoke(context, it) }
    }

    val root get() = container

    override fun get(meta: Meta): Output {
        return root.get(meta)
    }

    companion object {

        @JvmStatic
        fun display(): FXOutputManager = FXOutputManager()

        /**
         * Display in existing BorderPane
         */
        fun display(pane: BorderPane, meta: Meta = Meta.empty()): FXOutputManager =
            FXOutputManager(meta) { pane.center = it.root }
    }
}