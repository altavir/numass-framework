package inr.numass.tasks

import hep.dataforge.data.CustomDataFilter
import hep.dataforge.kodex.task
import hep.dataforge.tables.ListTable
import hep.dataforge.tables.Table
import inr.numass.data.analyzers.SmartAnalyzer
import inr.numass.data.api.NumassSet
import inr.numass.utils.NumassUtils

val selectDataTask = task("select") {
    model {
        data("*")
        configure(meta.getMetaOrEmpty("data"))
    }
    transform { data ->
        CustomDataFilter(meta).filter<NumassSet>(data.checked(NumassSet::class.java))
    }
}

val monitorTableTask = task("monitor") {
    model {
        dependsOn("select", meta)
    }
    join<NumassSet, Table> {
        result { data ->
            val monitorVoltage = meta.getDouble("monitorVoltage", 16000.0);
            val analyzer = SmartAnalyzer()
            val analyzerMeta = meta.getMetaOrEmpty("analyzer")
            val builder = ListTable.Builder("timestamp", "count", "cr", "crErr")
                    .rows(
                            data.values.stream().parallel()
                                    .flatMap { it.points }
                                    .filter { it.voltage == monitorVoltage }
                                    .map { it -> analyzer.analyzePoint(it, analyzerMeta) }
                    )

            context.io().out("numass.monitor", name).use {
                NumassUtils.write(it, meta, builder.build())
            }

            return@result builder.build();
        }
    }
}