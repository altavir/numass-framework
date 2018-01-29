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
import hep.dataforge.description.NodeDef
import hep.dataforge.description.TypedActionDef
import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import hep.dataforge.tables.*
import hep.dataforge.values.Values
import inr.numass.NumassUtils
import inr.numass.data.analyzers.NumassAnalyzer
import inr.numass.data.api.NumassPoint

import java.util.*

/**
 * @author Darksnake
 */
@TypedActionDef(name = "numass.merge", inputType = Table::class, outputType = Table::class, info = "Merge different numass data files into one.")
@NodeDef(name = "grouping", info = "The definition of grouping rule for this merge", from = "method::hep.dataforge.actions.GroupBuilder.byMeta")
class MergeDataAction : ManyToOneAction<Table, Table>() {

    private val parnames = arrayOf(NumassPoint.HV_KEY, NumassPoint.LENGTH_KEY, NumassAnalyzer.COUNT_KEY, NumassAnalyzer.COUNT_RATE_KEY, NumassAnalyzer.COUNT_RATE_ERROR_KEY)

    override fun buildGroups(context: Context, input: DataNode<Table>, actionMeta: Meta): List<DataNode<Table>> {
        val meta = inputMeta(context, input.getMeta(), actionMeta)
        val groups: List<DataNode<Table>>
        if (meta.hasValue("grouping.byValue")) {
            groups = super.buildGroups(context, input, actionMeta)
        } else {
            groups = GroupBuilder.byValue(MERGE_NAME, meta.getString(MERGE_NAME, input.name)).group(input)
        }
        return groups
    }

    override fun execute(context: Context, nodeName: String, data: Map<String, Table>, meta: Laminate): Table {
        val res = mergeDataSets(data.values)
        return ListTable(res.format, TableTransform.sort(res, NumassPoint.HV_KEY, true))
    }

    override fun afterGroup(context: Context, groupName: String, outputMeta: Meta, output: Table) {
        output(context, groupName) { stream -> NumassUtils.write(stream, outputMeta, output) }
    }

    private fun mergeDataPoints(dp1: Values?, dp2: Values?): Values? {
        if (dp1 == null) {
            return dp2
        }
        if (dp2 == null) {
            return dp1
        }

        val voltage = dp1.getValue(NumassPoint.HV_KEY).doubleValue()
        val t1 = dp1.getValue(NumassPoint.LENGTH_KEY).doubleValue()
        val t2 = dp2.getValue(NumassPoint.LENGTH_KEY).doubleValue()
        val time = t1 + t2

        val total = (dp1.getValue(NumassAnalyzer.COUNT_KEY).intValue() + dp2.getValue(NumassAnalyzer.COUNT_KEY).intValue()).toLong()

        val cr1 = dp1.getValue(NumassAnalyzer.COUNT_RATE_KEY).doubleValue()
        val cr2 = dp2.getValue(NumassAnalyzer.COUNT_RATE_KEY).doubleValue()

        val cr = (cr1 * t1 + cr2 * t2) / (t1 + t2)

        val err1 = dp1.getDouble(NumassAnalyzer.COUNT_RATE_ERROR_KEY)
        val err2 = dp2.getDouble(NumassAnalyzer.COUNT_RATE_ERROR_KEY)

        // абсолютные ошибки складываются квадратично
        val crErr = Math.sqrt(err1 * err1 * t1 * t1 + err2 * err2 * t2 * t2) / time

        return ValueMap.of(parnames, voltage, time, total, cr, crErr)
    }

    private fun mergeDataSets(ds: Collection<Table>): Table {
        //Сливаем все точки в один набор данных
        val points = LinkedHashMap<Double, MutableList<Values>>()
        for (d in ds) {
            if (!d.format.names.contains(*parnames)) {
                throw IllegalArgumentException()
            }
            for (dp in d) {
                val uset = dp.getValue(NumassPoint.HV_KEY).doubleValue()
                if (!points.containsKey(uset)) {
                    points.put(uset, ArrayList())
                }
                points[uset]?.add(dp)
            }
        }

        val res = ArrayList<Values>()

        points.entries.stream().map<Values> { entry ->
            var curPoint: Values? = null
            for (newPoint in entry.value) {
                curPoint = mergeDataPoints(curPoint, newPoint)
            }
            curPoint
        }.forEach { res.add(it) }

        return ListTable(MetaTableFormat.forNames(parnames), res)

    }

    companion object {
        val MERGE_NAME = "mergeName"
    }

}
