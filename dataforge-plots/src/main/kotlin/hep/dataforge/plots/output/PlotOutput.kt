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

package hep.dataforge.plots.output

import hep.dataforge.context.Context
import hep.dataforge.io.output.Output
import hep.dataforge.io.render
import hep.dataforge.meta.Configurable
import hep.dataforge.meta.KMetaBuilder
import hep.dataforge.plots.Plot
import hep.dataforge.plots.Plot.Companion.PLOT_TYPE
import hep.dataforge.plots.PlotFrame
import hep.dataforge.plots.Plottable
import hep.dataforge.plots.VirtualPlotFrame

interface PlotOutput : Output, Configurable {
    val frame: PlotFrame
}

fun Context.plot(plottable: Plottable, name: String? = null, stage: String? = null, transform: KMetaBuilder.() -> Unit = {}) {
    output.render(plottable, stage, name, Plot.PLOT_TYPE, transform)
}

fun Context.plot(plottables: Iterable<Plottable>, name: String? = null, stage: String? = null, transform: KMetaBuilder.() -> Unit = {}) {
    output.render(plottables, stage, name, Plot.PLOT_TYPE, transform)
}

fun Context.plotFrame(name: String, stage: String = "", action: PlotFrame.() -> Unit) {
    val frame = VirtualPlotFrame().apply(action)
    output[stage, name, PLOT_TYPE].render(frame)
}
