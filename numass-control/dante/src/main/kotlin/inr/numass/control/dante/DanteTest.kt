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

package inr.numass.control.dante

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.fx.plots.FXPlotManager
import hep.dataforge.kodex.KMetaBuilder
import hep.dataforge.kodex.buildMeta
import hep.dataforge.kodex.configure
import hep.dataforge.kodex.nullable
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.tables.Adapters
import inr.numass.data.analyzers.NumassAnalyzer
import inr.numass.data.analyzers.SimpleAnalyzer
import inr.numass.data.analyzers.withBinning
import inr.numass.data.api.NumassBlock
import inr.numass.data.channel
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) {
    val client = DanteClient("192.168.111.120", 7)
    client.open()
    val meta = buildMeta {
        "gain" to 1.0
        "detection_threshold" to 150
        "pileup_threshold" to 1
        "energy_filter" to {
            "peaking_time" to 63
            "flat_top" to 2
        }
        "fast_filter" to {
            "peaking_time" to 4
            "flat_top" to 1
        }
        "recovery_time" to 100
        "zero_peak_rate" to 0
    }
    runBlocking {
        println("Firmware version: ${client.getFirmwareVersion()}")

//        client.reset()
//        delay(500)

        client.configureAll(meta)

        val point = client.readPoint(10 * 1000)

        println("***META***")
        println(point.meta)
        println("***BLOCKS***")
        point.blocks.forEach {
            println("channel: ${it.channel}")
            println("\tlength: ${it.length}")
            println("\tevents: ${it.events.count()}")
            it.plotAmplitudeSpectrum(plotName = it.channel.toString())
        }
    }
}

fun NumassBlock.plotAmplitudeSpectrum(plotName: String = "spectrum", frameName: String = "", context: Context = Global, metaAction: KMetaBuilder.() -> Unit = {}) {
    val meta = buildMeta("meta", metaAction)
    val plotManager = context.load(FXPlotManager::class)
    val binning = meta.getInt("binning", 20)
    val lo = meta.optNumber("window.lo").nullable?.toInt()
    val up = meta.optNumber("window.up").nullable?.toInt()
    val data = SimpleAnalyzer().getAmplitudeSpectrum(this, meta.getMetaOrEmpty("spectrum")).withBinning(binning, lo, up)
    plotManager.display(name = frameName) {
        val valueAxis = if (meta.getBoolean("normalize", false)) {
            NumassAnalyzer.COUNT_RATE_KEY
        } else {
            NumassAnalyzer.COUNT_KEY
        }
        plots.configure {
            "connectionType" to "step"
            "thickness" to 2
            "showLine" to true
            "showSymbol" to false
            "showErrors" to false
        }.setType(DataPlot::class)

        val plot = DataPlot.plot(
                plotName,
                Adapters.buildXYAdapter(NumassAnalyzer.CHANNEL_KEY, valueAxis),
                data
        )
        plot.configure(meta)
        add(plot)
    }
}