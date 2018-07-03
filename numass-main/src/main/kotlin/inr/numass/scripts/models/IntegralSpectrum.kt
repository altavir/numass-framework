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

import hep.dataforge.fx.output.FXOutputManager
import hep.dataforge.kodex.buildContext
import hep.dataforge.kodex.configure
import hep.dataforge.kodex.step
import hep.dataforge.meta.Meta
import hep.dataforge.plots.Plot
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.jfreechart.JFreeChartPlugin
import hep.dataforge.plots.output.plot
import hep.dataforge.stat.fit.ParamSet
import inr.numass.NumassPlugin
import inr.numass.models.NBkgSpectrum
import inr.numass.models.sterile.SterileNeutrinoSpectrum


fun main(args: Array<String>) {

    val context = buildContext("NUMASS", NumassPlugin::class.java, JFreeChartPlugin::class.java) {
        output = FXOutputManager()
        rootDir = "D:\\Work\\Numass\\sterile2018_04"
        dataDir = "D:\\Work\\Numass\\data\\2018_04"
    }

    val sp = SterileNeutrinoSpectrum(context, Meta.empty())
    //beta.setCaching(false);

    val spectrum = NBkgSpectrum(sp)
    //val model = XYModel(Meta.empty(), SpectrumAdapter(Meta.empty()), spectrum)

    val params = ParamSet().apply {
        setPar("N", 2e6 / 100, 6.0, 0.0, Double.POSITIVE_INFINITY)
        setPar("bkg", 2.0, 0.03)
        setPar("E0", 18575.0, 1.0)
        setPar("mnu2", 0.0, 1.0)
        setParValue("msterile2", (8000 * 8000).toDouble())
        setPar("U2", 0.0, 1e-3)
        setPar("X", 0.1, 0.01)
        setPar("trap", 1.0, 0.01)
    }

    fun plotSpectrum(name: String, vararg override: Pair<String, Double>): Plot {
        val pars = params.copy().apply {
            override.forEach {
                setParValue(it.first, it.second)
            }
        }
        val x = (14000.0..18600.0).step(100.0).toList()
        val y = x.map { spectrum.value(it, pars) }
        return DataPlot.plot(name, x.toDoubleArray(), y.toDoubleArray())
    }



    context.plot("default") {
        plots.configure {
            "showLine" to true
            "showSymbol" to false
            "showErrors" to false
        }
        add(plotSpectrum("base"))
        add(plotSpectrum("noTrap", "trap" to 0.0))
    }

}