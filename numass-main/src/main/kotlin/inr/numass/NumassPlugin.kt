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

import hep.dataforge.context.*
import hep.dataforge.fx.FXPlugin
import hep.dataforge.fx.plots.PlotContainer
import hep.dataforge.maths.functions.FunctionLibrary
import hep.dataforge.meta.Meta
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import hep.dataforge.providers.Provides
import hep.dataforge.providers.ProvidesNames
import hep.dataforge.stat.models.ModelLibrary
import hep.dataforge.stat.models.WeightedXYModel
import hep.dataforge.stat.models.XYModel
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.ValuesAdapter
import hep.dataforge.workspace.tasks.Task
import inr.numass.data.analyzers.NumassAnalyzer
import inr.numass.data.api.NumassPoint
import inr.numass.models.*
import inr.numass.models.sterile.SterileNeutrinoSpectrum
import inr.numass.tasks.*
import org.apache.commons.math3.analysis.BivariateFunction
import org.apache.commons.math3.util.FastMath
import kotlin.math.pow

/**
 * @author Alexander Nozik
 */
@PluginDef(
    group = "inr.numass",
    name = "numass",
    dependsOn = ["hep.dataforge:functions", "hep.dataforge:MINUIT", "hep.dataforge:actions"],
    support = false,
    info = "Numass data analysis tools"
)
class NumassPlugin : BasicPlugin() {

    override fun attach(context: Context) {
        //        StorageManager.buildFrom(context);
        super.attach(context)
        //TODO Replace by local providers
        loadModels(context.getOrLoad(ModelLibrary::class.java))
        loadMath(FunctionLibrary.buildFrom(context))
    }

    private val tasks = listOf(
        NumassFitScanSummaryTask,
        NumassFitSummaryTask,
        selectTask,
        analyzeTask,
        mergeTask,
        mergeEmptyTask,
        monitorTableTask,
        subtractEmptyTask,
        transformTask,
        filterTask,
        fitTask,
        plotFitTask,
        histogramTask,
        fitScanTask,
        sliceTask,
        subThresholdTask
    )

    @Provides(Task.TASK_TARGET)
    fun getTask(name: String): Task<*>? {
        return tasks.find { it.name == name }
    }

    @ProvidesNames(Task.TASK_TARGET)
    fun taskList(): List<String> {
        return tasks.map { it.name }
    }

    private fun loadMath(math: FunctionLibrary) {
        math.addBivariate("numass.trap.lowFields") { Ei, Ef -> 3.92e-5 * FastMath.exp(-(Ei - Ef) / 300.0) + 1.97e-4 - 6.818e-9 * Ei }

        math.addBivariate("numass.trap.nominal") { Ei, f ->
            //return 1.64e-5 * FastMath.exp(-(Ei - Ef) / 300d) + 1.1e-4 - 4e-9 * Ei;
            1.2e-4 - 4.5e-9 * Ei
        }

        math.addBivariateFactory("numass.resolutionTail") { meta ->
            val alpha = meta.getDouble("tailAlpha", 0.0)
            val beta = meta.getDouble("tailBeta", 0.0)
            BivariateFunction { E: Double, U: Double -> 1 - (E - U) * (alpha + E / 1000.0 * beta) / 1000.0 }
        }

        math.addBivariateFactory("numass.resolutionTail.2017") { meta ->
            BivariateFunction { E: Double, U: Double ->
                val D = E - U
                0.99797 - 3.05346E-7 * D - 5.45738E-10 * D.pow(2.0) - 6.36105E-14 * D.pow(3.0)
            }
        }

        math.addBivariateFactory("numass.resolutionTail.2017.mod") { meta ->
            BivariateFunction { E: Double, U: Double ->
                val D = E - U
                val factor = 7.33 - E / 1000.0 / 3.0
                return@BivariateFunction 1.0 - (3.05346E-7 * D - 5.45738E-10 * D.pow(2.0) - 6.36105E-14 * D.pow(3.0)) * factor
            }
        }
    }


    /**
     * Load all numass model factories
     *
     * @param library
     */
    private fun loadModels(library: ModelLibrary) {

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

        library.addModel("scatter") { context, meta ->
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

        library.addModel("scatter-empiric") { context, meta ->
            val eGun = meta.getDouble("eGun", 19005.0)

            val interpolator = buildInterpolator(context, meta, eGun)

            val loss = EmpiricalLossSpectrum(interpolator, eGun + 5)
            val spectrum = NBkgSpectrum(loss)

            val weightReductionFactor = meta.getDouble("weightReductionFactor", 2.0)

            WeightedXYModel(meta, getAdapter(meta), spectrum) { dp -> weightReductionFactor }
        }

        library.addModel("scatter-empiric-variable") { context, meta ->
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

        library.addModel("scatter-analytic-variable") { context, meta ->
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

        library.addModel("scatter-empiric-experimental") { context, meta ->
            val eGun = meta.getDouble("eGun", 19005.0)

            //builder transmisssion with given data, annotation and smoothing
            val interpolator = buildInterpolator(context, meta, eGun)

            val smoothing = meta.getDouble("lossSmoothing", 0.3)

            val loss = ExperimentalVariableLossSpectrum.withData(interpolator, eGun + 5, smoothing)

            val spectrum = NBkgSpectrum(loss)

            val weightReductionFactor = meta.getDouble("weightReductionFactor", 2.0)

            WeightedXYModel(meta, getAdapter(meta), spectrum) { dp -> weightReductionFactor }
        }

        library.addModel("sterile") { context, meta ->
            val sp = SterileNeutrinoSpectrum(context, meta)
            val spectrum = NBkgSpectrum(sp)

            XYModel(meta, getAdapter(meta), spectrum)
        }

        library.addModel("sterile-corrected") { context, meta ->
            val sp = SterileNeutrinoSpectrum(context, meta)
            val spectrum = NBkgSpectrumWithCorrection(sp)

            XYModel(meta, getAdapter(meta), spectrum)
        }

        library.addModel("gun") { context, meta ->
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
                return TransmissionInterpolator.fromAction(
                    context,
                    transBuild,
                    transXName,
                    transYName,
                    nSmooth,
                    w,
                    stitchBorder
                )
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
            Adapters.buildXYAdapter(
                NumassPoint.HV_KEY,
                NumassAnalyzer.COUNT_RATE_KEY,
                NumassAnalyzer.COUNT_RATE_ERROR_KEY
            )
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
fun displayChart(
    title: String,
    context: Context = Global,
    width: Double = 800.0,
    height: Double = 600.0,
    meta: Meta = Meta.empty()
): JFreeChartFrame {
    val frame = JFreeChartFrame()
    frame.configure(meta)
    frame.configureValue("title", title)
    context.plugins.load<FXPlugin>().display(PlotContainer(frame), width, height)
    return frame
}
