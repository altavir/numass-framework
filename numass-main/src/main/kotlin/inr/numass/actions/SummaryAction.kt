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
package inr.numass.actions

import hep.dataforge.actions.GroupBuilder
import hep.dataforge.actions.ManyToOneAction
import hep.dataforge.context.Context
import hep.dataforge.data.DataNode
import hep.dataforge.description.TypedActionDef
import hep.dataforge.description.ValueDef
import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import hep.dataforge.stat.fit.FitState
import hep.dataforge.tables.ListTable
import hep.dataforge.tables.MetaTableFormat
import hep.dataforge.tables.Table
import hep.dataforge.tables.ValueMap
import hep.dataforge.values.Value
import inr.numass.NumassUtils
import java.util.*

/**
 * @author Darksnake
 */
@TypedActionDef(name = "summary", inputType = FitState::class, outputType = Table::class, info = "Generate summary for fit results of different datasets.")
@ValueDef(name = "parnames", multiple = true, required = true, info = "List of names of parameters for which summary should be done")
object SummaryAction : ManyToOneAction<FitState, Table>() {

    const val SUMMARY_NAME = "sumName"

    override fun buildGroups(context: Context, input: DataNode<FitState>, actionMeta: Meta): List<DataNode<FitState>> {
        val meta = inputMeta(context, input.meta, actionMeta)
        val groups: List<DataNode<FitState>>
        if (meta.hasValue("grouping.byValue")) {
            groups = super.buildGroups(context, input, actionMeta)
        } else {
            groups = GroupBuilder.byValue(SUMMARY_NAME, meta.getString(SUMMARY_NAME, "summary")).group<FitState>(input)
        }
        return groups
    }

    override fun execute(context: Context, nodeName: String, input: Map<String, FitState>, meta: Laminate): Table {
        val parNames: Array<String>
        if (meta.hasValue("parnames")) {
            parNames = meta.getStringArray("parnames")
        } else {
            throw RuntimeException("Infering parnames not suppoerted")
        }
        val names = arrayOfNulls<String>(2 * parNames.size + 2)
        names[0] = "file"
        for (i in parNames.indices) {
            names[2 * i + 1] = parNames[i]
            names[2 * i + 2] = parNames[i] + "Err"
        }
        names[names.size - 1] = "chi2"

        val res = ListTable.Builder(MetaTableFormat.forNames(names))

        val weights = DoubleArray(parNames.size)
        Arrays.fill(weights, 0.0)
        val av = DoubleArray(parNames.size)
        Arrays.fill(av, 0.0)

        input.forEach { key: String, value: FitState ->
            val values = arrayOfNulls<Value>(names.size)
            values[0] = Value.of(key)
            for (i in parNames.indices) {
                val `val` = Value.of(value.parameters.getDouble(parNames[i]))
                values[2 * i + 1] = `val`
                val err = Value.of(value.parameters.getError(parNames[i]))
                values[2 * i + 2] = err
                val weight = 1.0 / err.getDouble() / err.getDouble()
                av[i] += `val`.getDouble() * weight
                weights[i] += weight
            }
            values[values.size - 1] = Value.of(value.chi2)
            val point = ValueMap.of(names, *values)
            res.row(point)
        }

        val averageValues = arrayOfNulls<Value>(names.size)
        averageValues[0] = Value.of("average")
        averageValues[averageValues.size - 1] = Value.of(0)

        for (i in parNames.indices) {
            averageValues[2 * i + 1] = Value.of(av[i] / weights[i])
            averageValues[2 * i + 2] = Value.of(1 / Math.sqrt(weights[i]))
        }

        res.row(ValueMap.of(names, *averageValues))

        return res.build()
    }

    override fun afterGroup(context: Context, groupName: String, outputMeta: Meta, output: Table) {
        context.io.output(groupName, name).push(NumassUtils.wrap(output, outputMeta))
        super.afterGroup(context, groupName, outputMeta, output)
    }

}
