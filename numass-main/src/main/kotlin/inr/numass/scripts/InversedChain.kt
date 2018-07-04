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

import hep.dataforge.description.Descriptors
import hep.dataforge.kodex.buildContext
import hep.dataforge.meta.buildMeta
import hep.dataforge.plots.data.DataPlot
import inr.numass.NumassPlugin
import inr.numass.data.NumassDataUtils
import inr.numass.data.analyzers.NumassAnalyzer.Companion.AMPLITUDE_ADAPTER
import inr.numass.data.analyzers.SmartAnalyzer
import inr.numass.data.analyzers.withBinning
import inr.numass.data.api.NumassSet
import inr.numass.data.storage.NumassStorageFactory
import inr.numass.displayChart


fun main(args: Array<String>) {

    val context = buildContext("NUMASS", NumassPlugin::class.java) {
        rootDir = "D:\\Work\\Numass\\sterile\\2017_11"
        dataDir = "D:\\Work\\Numass\\data\\2017_11"
    }
    //val rootDir = File("D:\\Work\\Numass\\data\\2017_05\\Fill_2")

    val storage = NumassStorageFactory.buildLocal(context, "Fill_2", true, false);

    val sets = (10..24).map { "set_$it" }

    val loaders = sets.mapNotNull { set ->
        storage.provide("loader::$set", NumassSet::class.java).orElse(null)
    }

    val set = NumassDataUtils.join("sum", loaders)


    val analyzer = SmartAnalyzer()

    val meta = buildMeta {
        //        "t0" to 30e3
//        "inverted" to true
        "window.lo" to 400
        "window.up" to 1600
    }

    val metaForChain = meta.builder.setValue("t0", 15e3)

    val metaForChainInverted = metaForChain.builder.setValue("inverted", true)


    for (hv in arrayOf(14000.0, 14500.0, 15000.0, 15500.0, 16050.0)) {

        val frame = displayChart("integral[$hv]").apply {
            this.plots.descriptor = Descriptors.forType(DataPlot::class)
            this.plots.configureValue("showLine", true)
        }

        val point = set.optPoint(hv).get()

        frame.add(DataPlot.plot("raw", AMPLITUDE_ADAPTER, analyzer.getAmplitudeSpectrum(point, meta).withBinning(20)))
        frame.add(DataPlot.plot("filtered", AMPLITUDE_ADAPTER, analyzer.getAmplitudeSpectrum(point, metaForChain).withBinning(20)))
        frame.add(DataPlot.plot("invertedFilter", AMPLITUDE_ADAPTER, analyzer.getAmplitudeSpectrum(point, metaForChainInverted).withBinning(20)))
    }
}
