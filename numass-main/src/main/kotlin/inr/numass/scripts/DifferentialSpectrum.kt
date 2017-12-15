/*
 * Copyright  2017 Alexander Nozik.
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

package inr.numass.scripts

import hep.dataforge.fx.plots.PlotManager
import hep.dataforge.kodex.buildContext
import hep.dataforge.kodex.buildMeta
import hep.dataforge.kodex.replaceColumn
import hep.dataforge.plots.data.DataPlot
import inr.numass.NumassPlugin
import inr.numass.data.NumassDataUtils
import inr.numass.data.analyzers.NumassAnalyzer
import inr.numass.data.analyzers.SmartAnalyzer
import inr.numass.data.api.NumassSet
import inr.numass.data.storage.NumassStorageFactory

fun main(args: Array<String>) {

    val context = buildContext("NUMASS", NumassPlugin::class.java, PlotManager::class.java) {
        rootDir = "D:\\Work\\Numass\\sterile2017_05"
        dataDir = "D:\\Work\\Numass\\data\\2017_05"
    }
    //val rootDir = File("D:\\Work\\Numass\\data\\2017_05\\Fill_2")

    val storage = NumassStorageFactory.buildLocal(context, "Fill_2", true, false);

    val sets = (2..14).map { "set_$it" }

    val loaders = sets.mapNotNull { set ->
        storage.provide("loader::$set", NumassSet::class.java).orElse(null)
    }

    val analyzer = SmartAnalyzer()

    val all = NumassDataUtils.join("sum", loaders)


    val meta = buildMeta {
        "window.lo" to 400
        "window.up" to 1800
    }

    val plots = context.getFeature(PlotManager::class.java)

    val frame = plots.getPlotFrame("differential")

    val integralFrame = plots.getPlotFrame("integral")

    for (hv in arrayOf(14000.0, 14200.0, 14400.0, 14600.0, 14800.0, 15000.0)) {
        val point1 = all.optPoint(hv).get()

        val point0 = all.optPoint(hv + 200.0).get()

        with(NumassAnalyzer) {

            val spectrum1 = analyzer.getSpectrum(point1, meta).withBinning(20)

            val spectrum0 = analyzer.getSpectrum(point0, meta).withBinning(20)

            val res = subtractAmplitudeSpectrum(spectrum1, spectrum0)

            val norm = res.getColumn(COUNT_RATE_KEY).stream().mapToDouble { it.doubleValue() }.sum()

            integralFrame.add(DataPlot.plot("point_$hv", AMPLITUDE_ADAPTER, spectrum0))

            frame.add(DataPlot.plot("point_$hv", AMPLITUDE_ADAPTER, res.replaceColumn(COUNT_RATE_KEY) { getDouble(COUNT_RATE_KEY) / norm }))
        }
    }
}