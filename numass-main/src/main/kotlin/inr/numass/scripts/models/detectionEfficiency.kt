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
import hep.dataforge.data.NamedData
import hep.dataforge.fx.FXPlugin
import hep.dataforge.fx.output.FXOutputManager
import hep.dataforge.meta.Meta
import hep.dataforge.plots.Plot
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.jfreechart.JFreeChartPlugin
import hep.dataforge.plots.output.plotFrame
import hep.dataforge.plots.plotFunction
import hep.dataforge.stat.fit.FitHelper
import hep.dataforge.stat.fit.FitManager
import hep.dataforge.stat.fit.FitStage
import hep.dataforge.stat.fit.ParamSet
import hep.dataforge.stat.models.XYModel
import hep.dataforge.step
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.Table
import hep.dataforge.workspace.FileBasedWorkspace
import inr.numass.NumassPlugin
import inr.numass.data.analyzers.NumassAnalyzer
import inr.numass.data.api.NumassPoint
import inr.numass.models.NBkgSpectrum
import inr.numass.models.sterile.NumassResolution
import inr.numass.models.sterile.SterileNeutrinoSpectrum
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator
import java.io.File

private fun getCustomResolution(): NumassResolution {
    val correctionDataString = """
        10	0.376892
        12	0.1615868
        13	0.1009648
        14	0.0677539
        15	0.0487972
        16	0.037275
        17	0.02958922
        18	0.02511696
        19	0.02247986
    """.trimIndent()

    val correctionData = correctionDataString.lines().map { line ->
        val (u, cor) = line.split("\t")
        u.toDouble() * 1000 to cor.toDouble()
    }

    val correctionInterpolation = SplineInterpolator().interpolate(
        correctionData.map { it.first }.toDoubleArray(),
        correctionData.map { it.second }.toDoubleArray()
    )
    return NumassResolution(tailFunction = { e, _ -> 1.0 - correctionInterpolation.value(e) })
}

private fun XYModel.plot(name: String, params: ParamSet): Plot {
    val x = (14000.0..19000.0).step(100.0).toList()
    val y = x.map { spectrum.value(it, params) }
    return DataPlot.plot(name, x.toDoubleArray(), y.toDoubleArray())
}

fun main() {

    val context = buildContext(
        "NUMASS",
        NumassPlugin::class.java,
        FXPlugin::class.java,
        JFreeChartPlugin::class.java
    ) {
        output = FXOutputManager()
        properties {
            setValue("cache.enabled", false)
        }
    }

    val params = ParamSet().apply {
        setPar("N", 8e5, 1e4, 0.0, Double.POSITIVE_INFINITY)
        setPar("bkg", 3.0, 0.03)
        setPar("E0", 18575.0, 1.0)
        setPar("mnu2", 0.0, 1.0)
        setParValue("msterile2", (1000 * 1000).toDouble())
        setPar("U2", 0.0, 1e-3)
        setPar("X", 0.1, 0.01)
        setPar("trap", 1.0, 0.01)
    }

    val customResolution = getCustomResolution()

    context.plotFrame("fit", stage = "resolution") {
        plots.configure {
            "showLine" to true
            "showSymbol" to false
            "showErrors" to false
            "thickness" to 2.0
        }
        plots.setType<DataPlot>()

        plotFunction("custom", 14000.0, 18000.0, 1000) { x ->
            customResolution.value(x, 14100.0, params)
        }

        val basicResolution = NumassResolution()
        plotFunction("basic", 14000.0, 18000.0, 1000) { x ->
            basicResolution.value(x, 14100.0, params)
        }
    }

    val dataSpectrum = NBkgSpectrum(SterileNeutrinoSpectrum(context, Meta.empty(), resolution = customResolution))

    val t = 30 * 50 // time in seconds per point

    val adapter = Adapters.buildXYAdapter(
        NumassPoint.HV_KEY,
        NumassAnalyzer.COUNT_RATE_KEY,
        NumassAnalyzer.COUNT_RATE_ERROR_KEY
    )

    val fm = context.getOrLoad(FitManager::class.java)
    val x = (14000.0..18500.0).step(100.0).toList()

    // define the data model
    val modifiedModel = XYModel(Meta.empty(), adapter, dataSpectrum)

//    // Simulating data via model
//    val generator = SpectrumGenerator(dataModel, params, 12316)
//    val configuration = x.map { ValueMap.ofPairs(X_AXIS to it, "time" to t) }
//    val simulatedData: Table = generator.generateData(configuration)
//
//    // Creating a model which does not know about distortion
    val modelSpectrum = NBkgSpectrum(SterileNeutrinoSpectrum(context, Meta.empty(), resolution = NumassResolution()))
    val fitModel = XYModel(Meta.empty(), adapter, modelSpectrum)
//
//    context.plotFrame("fit", stage = "plots") {
//        plots.configure {
//            "showLine" to true
//            "showSymbol" to false
//            "showErrors" to false
//            "thickness" to 4.0
//        }
//        plots.setType<DataPlot>()
//        +dataModel.plot("Data", params)
//        +fitModel.plot("Fit-start", params)
//    }
//
//    //fitting
//    val state = FitState(simulatedData, fitModel, params)
//    val res = fm.runStage(state, "QOW", FitStage.TASK_RUN, "N", "E0", "bkg")
//    res.printState(PrintWriter(System.out))
//    val resWithTrap = fm.runStage(state, "QOW", FitStage.TASK_RUN, "N", "E0", "bkg", "trap")
//    resWithTrap.printState(PrintWriter(System.out))
//    val resWithU2 = fm.runStage(resWithTrap.optState().get(), "QOW", FitStage.TASK_RUN, "N", "E0", "bkg", "trap", "U2")
//    resWithU2.printState(PrintWriter(System.out))

    // loading real data
    val configPath = File("D:\\Work\\Numass\\sterile2017_11\\workspace.groovy").toPath()
    val workspace = FileBasedWorkspace.build(context, configPath)

    val data = workspace.runTask("filter", "fill_2").first() as NamedData<Table>
    val table = data.get()

    //fitting
    FitHelper(context).fit(table).apply {
        model(modifiedModel)
        report("fit")
        params(params)
        stage("QOW", FitStage.TASK_RUN, "N", "E0", "bkg")
        stage("QOW", FitStage.TASK_RUN, "N", "E0", "bkg", "trap")
        stage("QOW", FitStage.TASK_RUN, "N", "E0", "bkg", "trap", "U2")
    }.run()

}