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

package inr.numass.scripts.models

import hep.dataforge.buildContext
import hep.dataforge.configure
import hep.dataforge.fx.output.FXOutputManager
import hep.dataforge.plots.Plot
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.jfreechart.JFreeChartPlugin
import hep.dataforge.stat.fit.ParamSet
import hep.dataforge.step
import inr.numass.NumassPlugin
import inr.numass.displayChart
import inr.numass.models.sterile.NumassBeta

fun main(args: Array<String>) {
    val context = buildContext("NUMASS", NumassPlugin::class.java, JFreeChartPlugin::class.java) {
        output = FXOutputManager()
    }

    val spectrum = NumassBeta()//NBkgSpectrum(SterileNeutrinoSpectrum(context, Meta.empty()))

    val t = 30 * 50 // time in seconds per point

    val params = ParamSet().apply {
        setPar("N", 8e5, 6.0, 0.0, Double.POSITIVE_INFINITY)
        setPar("bkg", 0.0, 0.03)
        setPar("E0", 18575.0, 1.0)
        setPar("mnu2", 0.0, 1.0)
        setParValue("msterile2", (2.6 * 2.6))
        setPar("U2", 0.0, 1e-3)
        setPar("X", 0.0, 0.01)
        setPar("trap", 0.0, 0.01)
    }

    fun ParamSet.update(vararg override: Pair<String, Double>): ParamSet = this.copy().also { set ->
        override.forEach {
            set.setParValue(it.first, it.second)
        }
    }

    fun plotSpectrum(name: String, vararg override: Pair<String, Double>): Plot {
        val x = (18569.0..18575.0).step(0.2).toList()
        val y = x.map { 1e12*spectrum.value(0.0,it, params.update(*override)) }
        return DataPlot.plot(name, x.toDoubleArray(), y.toDoubleArray())
    }

    val frame = displayChart("Light neutrinos", context = context).apply {
        plots.setType<DataPlot>()
        plots.configure {
            "showSymbol" to false
            "showLine" to true
            "showErrors" to false
            "thickness" to 2
        }
    }

    frame.add(plotSpectrum("zero mass"))
    frame.add(plotSpectrum("active neutrino 2 ev", "mnu2" to 2.0))
    frame.add(plotSpectrum("sterile neutrino 2.6 ev", "U2" to 0.4).apply { configureValue("color", "red") })
    frame.add(plotSpectrum("sterile neutrino 2.6 ev, 0.09", "U2" to 0.09).apply { configureValue("color", "brown") })

}
