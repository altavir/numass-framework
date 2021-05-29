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

import hep.dataforge.configure
import hep.dataforge.context.Context
import hep.dataforge.meta.KMetaBuilder
import hep.dataforge.meta.buildMeta
import hep.dataforge.nullable
import hep.dataforge.plots.PlotFrame
import hep.dataforge.plots.PlotGroup
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.output.plotFrame
import hep.dataforge.tables.Adapters
import inr.numass.data.analyzers.NumassAnalyzer
import inr.numass.data.analyzers.SmartAnalyzer
import inr.numass.data.analyzers.withBinning
import inr.numass.data.api.NumassBlock

fun PlotGroup.plotAmplitudeSpectrum(
    numassBlock: NumassBlock,
    plotName: String = "spectrum",
    analyzer: NumassAnalyzer = SmartAnalyzer(),
    metaBuilder: KMetaBuilder.() -> Unit = {}
) {
    val meta = buildMeta("meta", metaBuilder)
    val binning = meta.getInt("binning", 20)
    val lo = meta.optNumber("window.lo").nullable?.toInt()
    val up = meta.optNumber("window.up").nullable?.toInt()
    val data = analyzer.getAmplitudeSpectrum(numassBlock, meta).withBinning(binning, lo, up)
    apply {
        val valueAxis = if (meta.getBoolean("normalize", false)) {
            NumassAnalyzer.COUNT_RATE_KEY
        } else {
            NumassAnalyzer.COUNT_KEY
        }
        configure {
            "connectionType" to "step"
            "thickness" to 2
            "showLine" to true
            "showSymbol" to false
            "showErrors" to false
        }.setType<DataPlot>()

        val plot = DataPlot.plot(
            plotName,
            data,
            Adapters.buildXYAdapter(NumassAnalyzer.CHANNEL_KEY, valueAxis)
        )
        plot.configure(meta)
        add(plot)
    }
}

fun PlotFrame.plotAmplitudeSpectrum(
    numassBlock: NumassBlock,
    plotName: String = "spectrum",
    analyzer: NumassAnalyzer = SmartAnalyzer(),
    metaBuilder: KMetaBuilder.() -> Unit = {}
) = plots.plotAmplitudeSpectrum(numassBlock, plotName, analyzer, metaBuilder)

fun Context.plotAmplitudeSpectrum(
    numassBlock: NumassBlock,
    plotName: String = "spectrum",
    frameName: String = plotName,
    analyzer: NumassAnalyzer = SmartAnalyzer(),
    metaAction: KMetaBuilder.() -> Unit = {}
) {
    plotFrame(frameName) {
        plotAmplitudeSpectrum(numassBlock, plotName, analyzer, metaAction)
    }
}

