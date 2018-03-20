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

package inr.numass.data

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.fx.plots.FXPlotManager
import hep.dataforge.kodex.KMetaBuilder
import hep.dataforge.kodex.buildMeta
import hep.dataforge.kodex.configure
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.tables.Adapters
import inr.numass.data.analyzers.NumassAnalyzer
import inr.numass.data.analyzers.SmartAnalyzer
import inr.numass.data.api.NumassBlock


fun NumassBlock.plotAmplitudeSpectrum(plotName: String = "spectrum", frameName: String = "", context: Context = Global, metaAction: KMetaBuilder.() -> Unit) {
    val meta = buildMeta("meta", metaAction)
    val plotManager = context.load(FXPlotManager::class)
    val data = SmartAnalyzer().getAmplitudeSpectrum(this, meta.getMetaOrEmpty("spectrum"))
    plotManager.display(name = frameName) {
        val valueAxis = if (meta.getBoolean("normalize",true)) {
            NumassAnalyzer.COUNT_RATE_KEY
        } else {
            NumassAnalyzer.COUNT_KEY
        }
         val plot = DataPlot.plot(
                plotName,
                Adapters.buildXYAdapter(NumassAnalyzer.CHANNEL_KEY, valueAxis),
                data
        ).configure {
            "connectionType" to "step"
            "thickness" to 2
            "showLine" to true
            "showSymbol" to false
            "showErrors" to false
            "JFreeChart.cache" to true
        }
        add(plot)
    }
}