/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.tasks

import hep.dataforge.data.DataNode
import hep.dataforge.data.DataTree
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.stat.fit.FitAction
import hep.dataforge.stat.fit.FitResult
import hep.dataforge.tables.Table
import hep.dataforge.values.ListValue
import hep.dataforge.values.Value
import hep.dataforge.workspace.tasks.AbstractTask
import hep.dataforge.workspace.tasks.TaskModel
import java.util.stream.Collectors

/**
 * @author Alexander Nozik
 */
object NumassFitScanTask : AbstractTask<FitResult>() {


    override fun run(model: TaskModel, data: DataNode<*>): DataNode<FitResult> {
        val config = model.meta
        val scanParameter = config.getString("parameter", "msterile2")

        val scanValues: Value = if (config.hasValue("masses")) {
            ListValue(config.getValue("masses")
                    .listValue().stream()
                    .map { it -> Math.pow(it.doubleValue() * 1000, 2.0) }
                    .collect(Collectors.toList<Any>())
            )
        } else {
            config.getValue("values", Value.of("[2.5e5, 1e6, 2.25e6, 4e6, 6.25e6, 9e6]"))
        }

        val action = FitAction()
        val resultBuilder = DataTree.edit(FitResult::class)
        val sourceNode = data.checked(Table::class.java)

        //do fit

        val fitConfig = config.getMeta("fit")
        sourceNode.dataStream().forEach { table ->
            for (i in 0 until scanValues.listValue().size) {
                val `val` = scanValues.listValue()[i]
                val overrideMeta = MetaBuilder(fitConfig)

                val resultName = String.format("%s[%s=%s]", table.name, scanParameter, `val`.stringValue())
                //                overrideMeta.setValue("@resultName", String.format("%s[%s=%s]", table.getName(), scanParameter, val.stringValue()));

                if (overrideMeta.hasMeta("params.$scanParameter")) {
                    overrideMeta.setValue("params.$scanParameter.value", `val`)
                } else {
                    overrideMeta.getMetaList("params.param").stream()
                            .filter { par -> par.getString("name") == scanParameter }
                            .forEach { it.setValue("value", `val`) }
                }
                //                Data<Table> newData = new Data<Table>(data.getGoal(),data.type(),overrideMeta);
                val node = action.run(model.context, DataNode.of(resultName, table, Meta.empty()), overrideMeta)
                resultBuilder.putData(table.name + ".fit_" + i, node.data!!)
            }
        }


        return resultBuilder.build()
    }

    override fun buildModel(model: TaskModel.Builder, meta: Meta) {
        model.configure(meta.getMetaOrEmpty("scan"))
        model.configure { it -> it.putNode(meta.getMetaOrEmpty("fit")) }
        model.dependsOn("filter", meta)
    }

    override val name = "fitscan"

}
