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
import hep.dataforge.meta.Meta
import hep.dataforge.plots.PlotPlugin
import hep.dataforge.plots.data.DataPlot
import inr.numass.NumassPlugin
import inr.numass.data.NumassDataUtils
import inr.numass.data.analyzers.NumassAnalyzer
import inr.numass.data.analyzers.TimeAnalyzer
import inr.numass.data.analyzers.getSpectrum
import inr.numass.data.api.NumassSet
import inr.numass.data.storage.NumassStorageFactory
import kotlin.streams.asSequence


fun main(args: Array<String>) {

    val context = buildContext("NUMASS", NumassPlugin::class.java, PlotManager::class.java){
        rootDir = "D:\\Work\\Numass\\sterile2017_05"
    }
    //val rootDir = File("D:\\Work\\Numass\\data\\2017_05\\Fill_2")

    val storage = NumassStorageFactory.buildLocal(context, "D:\\Work\\Numass\\data\\2017_05\\Fill_2", true, false);

    val sets = (2..14).map { "set_$it" }

    val loaders = sets.mapNotNull { set ->
        storage.provide("loader::$set", NumassSet::class.java).orElse(null)
    }

    val all = NumassDataUtils.join("sum", loaders)

    val point = all.optPoint(14000.0).get()

    val t0 = 20e3.toLong()

    val analyzer = TimeAnalyzer()

    val seconds = point.length.toMillis().toDouble() / 1000.0

    val binning = 20


    val plots = context.getFeature(PlotPlugin::class.java);

    val meta = buildMeta {
        node("window"){
            "lo" to 300
            "up" to 2600
        }
    }

    with(NumassAnalyzer) {
        val events = getSpectrum(seconds, analyzer.getEvents(point).asSequence(),meta)
                .withBinning(binning)

        val eventsNorming = events.getColumn(COUNT_RATE_KEY).stream().mapToDouble{it.doubleValue()}.sum()

        println("The norming factor for unfiltered count rate is $eventsNorming")

        val filtered = getSpectrum(
                seconds,
                analyzer.getEventsPairs(point, Meta.empty()).filter { it.second.timeOffset - it.first.timeOffset > t0 }.map { it.second },
                meta
        ).withBinning(binning)

        val filteredNorming = filtered.getColumn(COUNT_RATE_KEY).stream().mapToDouble{it.doubleValue()}.sum()

        println("The norming factor for filtered count rate is $filteredNorming")

        val defaultFiltered = getSpectrum(
                seconds,
                analyzer.getEvents(point, buildMeta {"t0" to t0}).asSequence(),
                meta
        ).withBinning(binning)

        val defaultFilteredNorming = defaultFiltered.getColumn(COUNT_RATE_KEY).stream().mapToDouble{it.doubleValue()}.sum()

        println("The norming factor for default filtered count rate is $defaultFilteredNorming")


        plots.getPlotFrame("amps").apply {
            add(DataPlot.plot("events", AMPLITUDE_ADAPTER, events.replaceColumn(COUNT_RATE_KEY){getDouble(COUNT_RATE_KEY)/eventsNorming}))
            add(DataPlot.plot("filtered", AMPLITUDE_ADAPTER, filtered.replaceColumn(COUNT_RATE_KEY){getDouble(COUNT_RATE_KEY)/filteredNorming}))
            add(DataPlot.plot("defaultFiltered", AMPLITUDE_ADAPTER, defaultFiltered.replaceColumn(COUNT_RATE_KEY){getDouble(COUNT_RATE_KEY)/defaultFilteredNorming}))
        }

//        plots.getPlotFrame("ratio").apply {
//
//            add(
//                    DataPlot.plot(
//                            "ratio",
//                            Adapters.DEFAULT_XY_ADAPTER,
//                            events.zip(filtered) { f, s ->
//                                Adapters.buildXYDataPoint(f.getDouble(CHANNEL_KEY), f.getDouble(COUNT_RATE_KEY) / s.getDouble(COUNT_RATE_KEY))
//                            }
//                    )
//            )
//        }
    }


}
