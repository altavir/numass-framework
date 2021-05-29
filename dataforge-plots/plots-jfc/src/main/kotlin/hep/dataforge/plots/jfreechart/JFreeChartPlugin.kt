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

package hep.dataforge.plots.jfreechart

import hep.dataforge.context.BasicPlugin
import hep.dataforge.context.Plugin
import hep.dataforge.context.PluginDef
import hep.dataforge.context.PluginFactory
import hep.dataforge.meta.Meta
import hep.dataforge.plots.PlotFactory
import hep.dataforge.plots.PlotFrame

@PluginDef(group = "hep.dataforge", name = "plots.jfreechart", dependsOn = ["hep.dataforge:fx"], info = "JFreeChart plot frame factory")
class JFreeChartPlugin : BasicPlugin(), PlotFactory {

    override fun build(meta: Meta): PlotFrame = JFreeChartFrame().apply { configure(meta) }


    class Factory : PluginFactory() {
        override val type: Class<out Plugin> = JFreeChartPlugin::class.java

        override fun build(meta: Meta): Plugin {
            return JFreeChartPlugin()
        }
    }
}

@Deprecated("To be replaced by outputs")
fun chart(transform: JFreeChartFrame.() -> Unit = {}): JFreeChartFrame {
    return JFreeChartFrame().apply(transform)
}