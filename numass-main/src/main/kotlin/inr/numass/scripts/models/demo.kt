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
import hep.dataforge.fx.output.FXOutputManager
import hep.dataforge.meta.Meta
import hep.dataforge.plots.jfreechart.JFreeChartPlugin
import hep.dataforge.stat.fit.ParamSet
import hep.dataforge.step
import inr.numass.NumassPlugin
import inr.numass.models.NBkgSpectrum
import inr.numass.models.misc.LossCalculator
import inr.numass.models.sterile.NumassTransmission
import inr.numass.models.sterile.SterileNeutrinoSpectrum
import kotlin.system.measureTimeMillis


fun main() {

    val context = buildContext("NUMASS", NumassPlugin::class.java, JFreeChartPlugin::class.java) {
        output = FXOutputManager()
    }

    val spectrum = NBkgSpectrum(SterileNeutrinoSpectrum(context, Meta.empty()))

    val params = ParamSet().apply {
        setPar("N", 8e5, 6.0, 0.0, Double.POSITIVE_INFINITY)
        setPar("bkg", 2.0, 0.03)
        setPar("E0", 18575.0, 1.0)
        setPar("mnu2", 0.0, 1.0)
        setParValue("msterile2", (1000 * 1000).toDouble())
        setPar("U2", 1e-2, 1e-3)
        setPar("X", 0.1, 0.01)
        setPar("trap", 1.0, 0.01)
    }

    println(spectrum.value(14000.0, params))
    val spectrumTime = measureTimeMillis {
        (14000.0..18600.0 step 10.0).forEach {
            println("$it\t${spectrum.value(it,params)}")
        }
    }
    println("Spectrum with 460 points computed in $spectrumTime millis")
}