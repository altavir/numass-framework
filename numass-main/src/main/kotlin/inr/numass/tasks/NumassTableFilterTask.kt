package inr.numass.tasks

import hep.dataforge.actions.Action
import hep.dataforge.actions.OneToOneAction
import hep.dataforge.context.Context
import hep.dataforge.data.DataNode
import hep.dataforge.description.TypedActionDef
import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import hep.dataforge.tables.Table
import hep.dataforge.tables.TableTransform
import hep.dataforge.values.ValueType
import hep.dataforge.values.Values
import hep.dataforge.workspace.tasks.SingleActionTask
import hep.dataforge.workspace.tasks.TaskModel
import inr.numass.data.api.NumassPoint
import inr.numass.utils.ExpressionUtils
import java.util.*

/**
 * Created by darksnake on 13-Aug-16.
 */
class NumassTableFilterTask : SingleActionTask<Table, Table>() {

    override fun getName(): String {
        return "filter"
    }

    override fun gatherNode(data: DataNode<*>): DataNode<Table> {
        return data.checked(Table::class.java)
    }


    override fun buildModel(model: TaskModel.Builder, meta: Meta) {
        if (meta.hasMeta("empty")) {
            model.dependsOn("dif", meta)
        } else {
            model.dependsOn("transform", meta)
        }
    }

    override fun getAction(model: TaskModel): Action<Table, Table> {
        return FilterTableAction()
    }

    @TypedActionDef(name = "filterTable", inputType = Table::class, outputType = Table::class)
    private inner class FilterTableAction : OneToOneAction<Table, Table>() {
        override fun execute(context: Context, name: String, input: Table, inputMeta: Laminate): Table {
            if (inputMeta.hasValue("from") || inputMeta.hasValue("to")) {
                val uLo = inputMeta.getDouble("from", 0.0)!!
                val uHi = inputMeta.getDouble("to", java.lang.Double.POSITIVE_INFINITY)!!
                getLogger(context, inputMeta).debug("Filtering finished")
                return TableTransform.filter(input, NumassPoint.HV_KEY, uLo, uHi)
            } else if (inputMeta.hasValue("condition")) {
                return TableTransform.filter(input) { ExpressionUtils.condition(inputMeta.getString("condition"), unbox(it)) }
            } else {
                throw RuntimeException("No filtering condition specified")
            }
        }
    }

    private fun unbox(dp: Values): Map<String, Any?> {
        val res = HashMap<String, Any?>()
        for (field in dp.names) {
            val value = dp.getValue(field)
            val obj: Any? = when (value.type) {
                ValueType.BOOLEAN -> value.booleanValue()
                ValueType.NUMBER -> value.doubleValue()
                ValueType.STRING -> value.stringValue()
                ValueType.TIME -> value.timeValue()
                ValueType.NULL -> null
            }
            res.put(field, obj)
        }
        return res
    }
}
