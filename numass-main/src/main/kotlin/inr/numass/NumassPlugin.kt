/* 
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package inr.numass

import hep.dataforge.actions.ActionManager
import hep.dataforge.context.*
import hep.dataforge.fx.FXPlugin
import hep.dataforge.fx.plots.PlotContainer
import hep.dataforge.maths.MathPlugin
import hep.dataforge.meta.Meta
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import hep.dataforge.stat.models.ModelManager
import hep.dataforge.stat.models.WeightedXYModel
import hep.dataforge.stat.models.XYModel
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.ValuesAdapter
import inr.numass.data.analyzers.NumassAnalyzer
import inr.numass.data.api.NumassPoint
import inr.numass.models.*
import inr.numass.models.sterile.SterileNeutrinoSpectrum
import inr.numass.tasks.*
import org.apache.commons.math3.analysis.BivariateFunction
import org.apache.commons.math3.util.FastMath

/**
 * @author Alexander Nozik
 */
@PluginDef(
        group = "inr.numass",
        name = "numass",
        dependsOn = arrayOf("hep.dataforge:math", "hep.dataforge:MINUIT", "hep.dataforge:actions", "hep.dataforge:io.dir"),
        support = false,
        info = "Numass data analysis tools"
)
class NumassPlugin : BasicPlugin() {

    override fun attach(context: Context) {
        //        StorageManager.buildFrom(context);
        super.attach(context)
        loadModels(context[ModelManager::class.java])
        loadMath(MathPlugin.buildFrom(context))

        context.get(ActionManager::class.java).apply {
            putTask(NumassFitScanTask::class.java)
            putTask(NumassFitScanSummaryTask::class.java)
            putTask(NumassFitSummaryTask::class.java)
            put(selectTask)
            put(analyzeTask)
            put(mergeTask)
            put(mergeEmptyTask)
            put(monitorTableTask)
            put(subtractEmptyTask)
            put(transformTask)
            put(filterTask)
            put(fitTask)
            put(plotFitTask)
        }

    }

    private fun loadMath(math: MathPlugin) {
        math.registerBivariate("numass.trap.lowFields") { Ei, Ef -> 3.92e-5 * FastMath.exp(-(Ei - Ef) / 300.0) + 1.97e-4 - 6.818e-9 * Ei }

        math.registerBivariate("numass.trap.nominal") { Ei, Ef ->
            //return 1.64e-5 * FastMath.exp(-(Ei - Ef) / 300d) + 1.1e-4 - 4e-9 * Ei;
            1.2e-4 - 4.5e-9 * Ei
        }

        math.registerBivariate("numass.resolutionTail") { meta ->
            val alpha = meta.getDouble("tailAlpha", 0.0)
            val beta = meta.getDouble("tailBeta", 0.0)
            BivariateFunction { E: Double, U: Double -> 1 - (E - U) * (alpha + E / 1000.0 * beta) / 1000.0 }
        }

        math.registerBivariate("numass.resolutionTail.2017") { meta ->
            BivariateFunction { E: Double, U: Double ->
                val D = E - U
                0.99797 - 3.05346E-7 * D - 5.45738E-10 * Math.pow(D, 2.0) - 6.36105E-14 * Math.pow(D, 3.0)
            }
        }

        math.registerBivariate("numass.resolutionTail.2017.mod") { meta ->
            BivariateFunction { E: Double, U: Double ->
                val D = E - U
                val factor = 7.33 - E / 1000.0 / 3.0
                return@BivariateFunction 1.0 - (3.05346E-7 * D - 5.45738E-10 * Math.pow(D, 2.0) - 6.36105E-14 * Math.pow(D, 3.0)) * factor
            }
        }
    }

    /**
     * Load all numass model factories
     *
     * @param manager
     */
    private fun loadModels(manager: ModelManager) {

        //        manager.addModel("modularbeta", (context, an) -> {
        //            double A = an.getDouble("resolution", 8.3e-5);//8.3e-5
        //            double from = an.getDouble("from", 14400d);
        //            double to = an.getDouble("to", 19010d);
        //            RangedNamedSetSpectrum beta = new BetaSpectrum(getClass().getResourceAsStream("/data/FS.txt"));
        //            ModularSpectrum sp = new ModularSpectrum(beta, A, from, to);
        //            NBkgSpectrum spectrum = new NBkgSpectrum(sp);
        //
        //            return new XYModel(spectrum, getAdapter(an));
        //        });

        manager.addModel("scatter") { context, meta ->
            val A = meta.getDouble("resolution", 8.3e-5)//8.3e-5
            val from = meta.getDouble("from", 0.0)
            val to = meta.getDouble("to", 0.0)

            val sp: ModularSpectrum
            sp = if (from == to) {
                ModularSpectrum(GaussSourceSpectrum(), A)
            } else {
                ModularSpectrum(GaussSourceSpectrum(), A, from, to)
            }

            val spectrum = NBkgSpectrum(sp)

            XYModel(meta, getAdapter(meta), spectrum)
        }

        manager.addModel("scatter-empiric") { context, meta ->
            val eGun = meta.getDouble("eGun", 19005.0)

            val interpolator = buildInterpolator(context, meta, eGun)

            val loss = EmpiricalLossSpectrum(interpolator, eGun + 5)
            val spectrum = NBkgSpectrum(loss)

            val weightReductionFactor = meta.getDouble("weightReductionFactor", 2.0)

            WeightedXYModel(meta, getAdapter(meta), spectrum) { dp -> weightReductionFactor }
        }

        manager.addModel("scatter-empiric-variable") { context, meta ->
            val eGun = meta.getDouble("eGun", 19005.0)

            //builder transmisssion with given data, annotation and smoothing
            val interpolator = buildInterpolator(context, meta, eGun)

            val loss = VariableLossSpectrum.withData(interpolator, eGun + 5)

            val tritiumBackground = meta.getDouble("tritiumBkg", 0.0)

            val spectrum: NBkgSpectrum
            if (tritiumBackground == 0.0) {
                spectrum = NBkgSpectrum(loss)
            } else {
                spectrum = CustomNBkgSpectrum.tritiumBkgSpectrum(loss, tritiumBackground)
            }

            val weightReductionFactor = meta.getDouble("weightReductionFactor", 2.0)

            WeightedXYModel(meta, getAdapter(meta), spectrum) { dp -> weightReductionFactor }
        }

        manager.addModel("scatter-analytic-variable") { context, meta ->
            val eGun = meta.getDouble("eGun", 19005.0)

            val loss = VariableLossSpectrum.withGun(eGun + 5)

            val tritiumBackground = meta.getDouble("tritiumBkg", 0.0)

            val spectrum: NBkgSpectrum
            if (tritiumBackground == 0.0) {
                spectrum = NBkgSpectrum(loss)
            } else {
                spectrum = CustomNBkgSpectrum.tritiumBkgSpectrum(loss, tritiumBackground)
            }

            XYModel(meta, getAdapter(meta), spectrum)
        }

        manager.addModel("scatter-empiric-experimental") { context, meta ->
            val eGun = meta.getDouble("eGun", 19005.0)

            //builder transmisssion with given data, annotation and smoothing
            val interpolator = buildInterpolator(context, meta, eGun)

            val smoothing = meta.getDouble("lossSmoothing", 0.3)

            val loss = ExperimentalVariableLossSpectrum.withData(interpolator, eGun + 5, smoothing)

            val spectrum = NBkgSpectrum(loss)

            val weightReductionFactor = meta.getDouble("weightReductionFactor", 2.0)

            WeightedXYModel(meta, getAdapter(meta), spectrum) { dp -> weightReductionFactor }
        }

        manager.addModel("sterile") { context, meta ->
            val sp = SterileNeutrinoSpectrum(context, meta)
            val spectrum = NBkgSpectrum(sp)

            XYModel(meta, getAdapter(meta), spectrum)
        }

        manager.addModel("gun") { context, meta ->
            val gsp = GunSpectrum()

            val tritiumBackground = meta.getDouble("tritiumBkg", 0.0)

            val spectrum: NBkgSpectrum
            if (tritiumBackground == 0.0) {
                spectrum = NBkgSpectrum(gsp)
            } else {
                spectrum = CustomNBkgSpectrum.tritiumBkgSpectrum(gsp, tritiumBackground)
            }

            XYModel(meta, getAdapter(meta), spectrum)
        }

    }

    private fun buildInterpolator(context: Context, an: Meta, eGun: Double): TransmissionInterpolator {
        val transXName = an.getString("transXName", "Uset")
        val transYName = an.getString("transYName", "CR")

        val stitchBorder = an.getDouble("stitchBorder", eGun - 7)
        val nSmooth = an.getInt("nSmooth", 15)

        val w = an.getDouble("w", 0.8)

        if (an.hasValue("transFile")) {
            val transmissionFile = an.getString("transFile")

            return TransmissionInterpolator
                    .fromFile(context, transmissionFile, transXName, transYName, nSmooth, w, stitchBorder)
        } else if (an.hasMeta("transBuildAction")) {
            val transBuild = an.getMeta("transBuildAction")
            try {
                return TransmissionInterpolator.fromAction(context,
                        transBuild, transXName, transYName, nSmooth, w, stitchBorder)
            } catch (ex: InterruptedException) {
                throw RuntimeException("Transmission builder failed")
            }

        } else {
            throw RuntimeException("Transmission declaration not found")
        }
    }

    private fun getAdapter(an: Meta): ValuesAdapter {
        return if (an.hasMeta(ValuesAdapter.ADAPTER_KEY)) {
            Adapters.buildAdapter(an.getMeta(ValuesAdapter.ADAPTER_KEY))
        } else {
            Adapters.buildXYAdapter(NumassPoint.HV_KEY, NumassAnalyzer.COUNT_RATE_KEY, NumassAnalyzer.COUNT_RATE_ERROR_KEY)
        }
    }

    class Factory : PluginFactory() {
        override val type: Class<out Plugin> = NumassPlugin::class.java

        override fun build(meta: Meta): Plugin {
            return NumassPlugin()
        }
    }
}

/**
 * Display a JFreeChart plot frame in a separate stage window
 *
 * @param title
 * @param width
 * @param height
 * @return
 */
@JvmOverloads
fun displayJFreeChart(title: String, width: Double = 800.0, height: Double = 600.0, meta: Meta = Meta.empty()): JFreeChartFrame {
    val frame = JFreeChartFrame(meta)
    frame.configureValue("title", title)
    FXPlugin().apply { startGlobal() }.display(PlotContainer(frame), width, height)
    return frame
}
