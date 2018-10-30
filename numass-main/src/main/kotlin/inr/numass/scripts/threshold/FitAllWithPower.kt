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

package inr.numass.scripts.threshold

import hep.dataforge.buildContext
import hep.dataforge.fx.output.FXOutputManager
import hep.dataforge.io.DirectoryOutput
import hep.dataforge.io.plus
import hep.dataforge.io.render
import hep.dataforge.meta.buildMeta
import hep.dataforge.nullable
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.jfreechart.JFreeChartPlugin
import hep.dataforge.plots.plotData
import hep.dataforge.storage.files.FileStorage
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.filter
import hep.dataforge.tables.sort
import inr.numass.NumassPlugin
import inr.numass.data.NumassDataUtils
import inr.numass.data.api.NumassSet
import inr.numass.data.storage.NumassDirectory
import inr.numass.displayChart
import inr.numass.subthreshold.Threshold

fun main(args: Array<String>) {
    val context = buildContext("NUMASS", NumassPlugin::class.java, JFreeChartPlugin::class.java) {
        rootDir = "D:\\Work\\Numass\\sterile\\2017_05"
        dataDir = "D:\\Work\\Numass\\data\\2017_05"
        output = FXOutputManager() + DirectoryOutput()
    }

    val storage = NumassDirectory.read(context, "Fill_2") as? FileStorage ?: error("Storage not found")

    val meta = buildMeta {
        "delta" to -150
        "method" to "pow"
        "t0" to 15e3
//        "window.lo" to 400
//        "window.up" to 1600
        "xLow" to 450
        "xHigh" to 700
        "upper" to 3100
        "binning" to 20
    }

    val frame = displayChart("correction").apply {
        plots.setType<DataPlot>()
    }

    val sets = (1..18).map { "set_$it" }.map { setName ->
        storage.provide(setName, NumassSet::class.java).nullable
    }.filterNotNull()

    val name = "fill_2[1-18]"

    val sum = NumassDataUtils.join(name, sets)

    val correctionTable = Threshold.calculateSubThreshold(sum, meta).filter {
        it.getDouble("correction") in (1.0..1.2)
    }.sort("voltage")

    frame.plotData("${name}_cor", correctionTable, Adapters.buildXYAdapter("U", "correction"))
    frame.plotData("${name}_a", correctionTable, Adapters.buildXYAdapter("U", "a"))
    frame.plotData("${name}_beta", correctionTable, Adapters.buildXYAdapter("U", "beta"))

    context.output.render(correctionTable,"numass.correction", "fill_2[1-18]")
}