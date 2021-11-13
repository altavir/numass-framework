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

import hep.dataforge.buildContext
import hep.dataforge.meta.buildMeta
import hep.dataforge.nullable
import hep.dataforge.plots.PlotGroup
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.tables.ColumnTable
import inr.numass.NumassPlugin
import inr.numass.data.analyzers.*
import inr.numass.data.analyzers.NumassAnalyzer.Companion.AMPLITUDE_ADAPTER
import inr.numass.data.api.NumassSet
import inr.numass.data.storage.NumassDirectory
import inr.numass.displayChart

/**
 * Investigation of gun data for time chain anomaliese
 */
fun main() {

    val context = buildContext("NUMASS", NumassPlugin::class.java) {
        rootDir = "D:\\Work\\Numass\\sterile\\2017_11"
        dataDir = "D:\\Work\\Numass\\data\\2017_11"
    }
    //val rootDir = File("D:\\Work\\Numass\\data\\2017_05\\Fill_2")

    val storage = NumassDirectory.read(context, "Adiabacity_19")!!

    val sets = listOf("set_2")


    val analyzer = SmartAnalyzer()

    val meta = buildMeta {
        //        "t0" to 30e3
//        "inverted" to true
        "window.lo" to 400
        "window.up" to 2600
    }

    val t0 = 15e3

    val metaForChain = meta.builder.apply {
        setValue("t0", t0)
        setValue("inverted", false)
    }

    val metaForChainInverted = metaForChain.builder.setValue("inverted", true)

    val hv = 18000.0

    val frame = displayChart("integral[$hv]").apply {
        this.plots.setType<DataPlot>()
        this.plots.configureValue("showLine", true)
    }

    val normalizedFrame = displayChart("normalized[$hv]").apply {
        this.plots.setType<DataPlot>()
        this.plots.configureValue("showLine", true)
    }

    sets.forEach { setName ->
        val set = storage.provide(setName, NumassSet::class.java).nullable ?: error("Set does not exist")

        val point = set.optPoint(hv).get()

        val group = PlotGroup(setName)
        frame.add(group)

        val rawSpectrum = analyzer.getAmplitudeSpectrum(point, meta).withBinning(20)
        group.add(DataPlot.plot("raw", rawSpectrum, AMPLITUDE_ADAPTER))

        val rawNorm = rawSpectrum.getColumn(NumassAnalyzer.COUNT_RATE_KEY).maxByOrNull { it.double }!!.double
        val normalizedSpectrum = ColumnTable.copy(rawSpectrum)
                .replaceColumn(NumassAnalyzer.COUNT_RATE_KEY) { it.getDouble(NumassAnalyzer.COUNT_RATE_KEY) / rawNorm }
        normalizedFrame.add(DataPlot.plot("${setName}_raw", normalizedSpectrum, AMPLITUDE_ADAPTER))


        println("[$setName] Raw spectrum integral: ${
            rawSpectrum.getColumn(NumassAnalyzer.COUNT_RATE_KEY).sumOf { it.double }
        }")

        group.add(DataPlot.plot("filtered", analyzer.getAmplitudeSpectrum(point, metaForChain).withBinning(20), AMPLITUDE_ADAPTER))

        val filteredSpectrum = analyzer.getAmplitudeSpectrum(point, metaForChainInverted).withBinning(20)
        group.add(DataPlot.plot("invertedFilter", filteredSpectrum, AMPLITUDE_ADAPTER))

        val filteredNorm = filteredSpectrum.getColumn(NumassAnalyzer.COUNT_RATE_KEY).maxByOrNull { it.double }!!.double
        val normalizedFilteredSpectrum = ColumnTable.copy(filteredSpectrum)
                .replaceColumn(NumassAnalyzer.COUNT_RATE_KEY) { it.getDouble(NumassAnalyzer.COUNT_RATE_KEY) / filteredNorm }

        normalizedFrame.add(DataPlot.plot(setName, normalizedFilteredSpectrum, AMPLITUDE_ADAPTER))

        val sequence = TimeAnalyzer()
                .getEventsWithDelay(point, metaForChainInverted)
                .filter { pair -> pair.second <= t0 }
                .map { it.first }

        val pileupSpectrum = sequence.getAmplitudeSpectrum(point.length.toMillis().toDouble() / 1000.0).withBinning(20)

        group.add(DataPlot.plot("pileup", pileupSpectrum, AMPLITUDE_ADAPTER))

        println("[$setName] Pileup spectrum integral: ${
            pileupSpectrum.getColumn(NumassAnalyzer.COUNT_RATE_KEY).sumOf { it.double }
        }")
    }
}
