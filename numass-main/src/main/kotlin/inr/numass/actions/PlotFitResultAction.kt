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
package inr.numass.actions

import hep.dataforge.actions.OneToOneAction
import hep.dataforge.context.Context
import hep.dataforge.description.NodeDef
import hep.dataforge.description.TypedActionDef
import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import hep.dataforge.plots.XYFunctionPlot
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.output.plot
import hep.dataforge.stat.fit.FitResult
import hep.dataforge.stat.fit.FitState
import hep.dataforge.stat.models.XYModel
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.ValuesAdapter
import java.util.stream.StreamSupport

/**
 * @author darksnake
 */
@TypedActionDef(name = "plotFit", info = "Plot fit result", inputType = FitState::class, outputType = FitState::class)
@NodeDef(key = "adapter", info = "adapter for DataSet being fitted. By default is taken from model.")
object PlotFitResultAction : OneToOneAction<FitResult, FitResult>() {

    override fun execute(context: Context, name: String, input: FitResult, metaData: Laminate): FitResult {

        val state = input.optState().orElseThrow { UnsupportedOperationException("Can't work with fit result not containing state, sorry! Will fix it later") }

        val data = input.data
        if (state.model !is XYModel) {
            context.history.getChronicle(name).reportError("The fit model should be instance of XYModel for this action. Action failed!")
            return input
        }
        val model = state.model as XYModel

        val adapter: ValuesAdapter
        if (metaData.hasMeta("adapter")) {
            adapter = Adapters.buildAdapter(metaData.getMeta("adapter"))
        } else if (state.model is XYModel) {
            adapter = model.adapter
        } else {
            throw RuntimeException("No adapter defined for data interpretation")
        }

//        val frame = PlotOutputKt.getPlotFrame(context, getName(), name, metaData.getMeta("frame", Meta.empty()))


        val fit = XYFunctionPlot("fit", Meta.empty()) { x: Double -> model.spectrum.value(x, input.parameters) }
        fit.density = 100
        fit.smoothing = true
        // ensuring all data points are calculated explicitly
        StreamSupport.stream(data.spliterator(), false)
                .map { dp -> Adapters.getXValue(adapter, dp).double }.sorted().forEach{ fit.calculateIn(it) }

        context.plot(listOf(fit,DataPlot.plot("data", adapter, data)), this.name, name)

        return input
    }

}
