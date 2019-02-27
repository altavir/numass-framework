package inr.numass.tasks

import hep.dataforge.io.render
import hep.dataforge.plots.data.DataPlot
import hep.dataforge.plots.output.plotFrame
import hep.dataforge.plots.plotData
import hep.dataforge.tables.Adapters
import hep.dataforge.tables.Table
import hep.dataforge.tables.filter
import hep.dataforge.useMeta
import hep.dataforge.values.ValueType
import hep.dataforge.workspace.tasks.task
import inr.numass.data.NumassDataUtils
import inr.numass.data.api.NumassSet
import inr.numass.subthreshold.Threshold

val subThresholdTask = task("threshold") {
    descriptor {
        value("plot", types = listOf(ValueType.BOOLEAN), defaultValue = false, info = "Show threshold correction plot")
        value(
            "binning",
            types = listOf(ValueType.NUMBER),
            defaultValue = 16,
            info = "The binning used for fit"
        )
        info = "Calculate sub threshold correction"
    }
    model { meta ->
        dependsOn(selectTask, meta)
        configure(meta.getMetaOrEmpty("threshold"))
        configure {
            meta.useMeta("analyzer") { putNode(it) }
            setValue("@target", meta.getString("@target", meta.name))
        }
    }
    join<NumassSet, Table> { data ->
        val sum = NumassDataUtils.joinByIndex(name, data.values)

        val correctionTable = Threshold.calculateSubThreshold(sum, meta).filter {
            it.getDouble("correction") in (1.0..1.2)
        }

        if (meta.getBoolean("plot", false)) {
            context.plotFrame("$name.plot", stage = "numass.threshold") {
                plots.setType<DataPlot>()
                plotData("${name}_cor", correctionTable, Adapters.buildXYAdapter("U", "correction"))
                plotData("${name}_a", correctionTable, Adapters.buildXYAdapter("U", "a"))
                plotData("${name}_beta", correctionTable, Adapters.buildXYAdapter("U", "beta"))
            }
        }

        context.output.render(correctionTable, "numass.correction", name, meta = meta)
        return@join correctionTable
    }
}
