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
import hep.dataforge.description.Descriptors
import hep.dataforge.meta.buildMeta
import hep.dataforge.plots.data.DataPlot
import inr.numass.NumassPlugin
import inr.numass.data.analyzers.NumassAnalyzer.Companion.AMPLITUDE_ADAPTER
import inr.numass.data.analyzers.SmartAnalyzer
import inr.numass.data.analyzers.withBinning
import inr.numass.data.storage.ProtoNumassPoint
import inr.numass.displayChart
import java.nio.file.Paths


fun main(args: Array<String>) {

    val context = buildContext("NUMASS", NumassPlugin::class.java)

    val analyzer = SmartAnalyzer()

    val meta = buildMeta {
        "window.lo" to 800
        "window.up" to 5600
    }

    val metaForChain = meta.builder.setValue("t0", 15e3)

    val metaForChainInverted = metaForChain.builder.setValue("inverted", true)

    val point = ProtoNumassPoint.readFile(Paths.get("D:\\Work\\Numass\\data\\2017_05_frames\\Fill_3_events\\set_33\\p36(30s)(HV1=17000).df"))

    val frame = displayChart("integral").apply {
        this.plots.descriptor = Descriptors.forType(DataPlot::class)
        this.plots.configureValue("showLine", true)
    }

    frame.add(DataPlot.plot("raw", analyzer.getAmplitudeSpectrum(point, meta).withBinning(80), AMPLITUDE_ADAPTER))
    frame.add(DataPlot.plot("filtered", analyzer.getAmplitudeSpectrum(point, metaForChain).withBinning(80), AMPLITUDE_ADAPTER))
    frame.add(DataPlot.plot("invertedFilter", analyzer.getAmplitudeSpectrum(point, metaForChainInverted).withBinning(80), AMPLITUDE_ADAPTER))

}
