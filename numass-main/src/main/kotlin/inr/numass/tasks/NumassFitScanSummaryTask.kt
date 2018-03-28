/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.tasks

import hep.dataforge.actions.ManyToOneAction
import hep.dataforge.context.Context
import hep.dataforge.data.DataNode
import hep.dataforge.data.DataSet
import hep.dataforge.description.TypedActionDef
import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import hep.dataforge.stat.fit.FitResult
import hep.dataforge.stat.fit.UpperLimitGenerator
import hep.dataforge.tables.ListTable
import hep.dataforge.tables.Table
import hep.dataforge.tables.TableTransform
import hep.dataforge.workspace.tasks.AbstractTask
import hep.dataforge.workspace.tasks.TaskModel
import inr.numass.NumassUtils

/**
 * @author Alexander Nozik
 */
object NumassFitScanSummaryTask : AbstractTask<Table>() {

    override fun run(model: TaskModel, data: DataNode<*>): DataNode<Table> {
        val builder = DataSet.edit(Table::class)
        val action = FitSummaryAction()
        val input = data.checked(FitResult::class.java)
        input.nodeStream()
                .filter { it -> it.getSize(false) > 0 }
                .forEach { node -> builder.putData(node.name, action.run(model.context, node, model.meta).data!!) }
        return builder.build()
    }

    override fun buildModel(model: TaskModel.Builder, meta: Meta) {
        model.dependsOn("fitscan", meta)
    }


    override val name = "scansum"

    @TypedActionDef(name = "sterileSummary", inputType = FitResult::class, outputType = Table::class)
    private class FitSummaryAction : ManyToOneAction<FitResult, Table>() {

        override fun execute(context: Context, nodeName: String, input: Map<String, FitResult>, meta: Laminate): Table {
            val builder = ListTable.Builder("m", "U2", "U2err", "U2limit", "E0", "trap")
            input.forEach { key, fitRes ->
                val pars = fitRes.parameters

                val u2Val = pars.getDouble("U2") / pars.getError("U2")

                val limit: Double = if (Math.abs(u2Val) < 3) {
                    UpperLimitGenerator.getConfidenceLimit(u2Val) * pars.getError("U2")
                } else {
                    java.lang.Double.NaN
                }

                builder.row(
                        Math.sqrt(pars.getValue("msterile2").doubleValue()),
                        pars.getValue("U2"),
                        pars.getError("U2"),
                        limit,
                        pars.getValue("E0"),
                        pars.getValue("trap"))
            }
            val res = TableTransform.sort(builder.build(), "m", true)
            context.io.output(nodeName, stage = name).push(NumassUtils.wrap(res, meta))
            return res
        }

    }

}
