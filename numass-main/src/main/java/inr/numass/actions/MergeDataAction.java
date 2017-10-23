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
package inr.numass.actions;

import hep.dataforge.actions.GroupBuilder;
import hep.dataforge.actions.ManyToOneAction;
import hep.dataforge.context.Context;
import hep.dataforge.data.DataNode;
import hep.dataforge.description.NodeDef;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.meta.Laminate;
import hep.dataforge.meta.Meta;
import hep.dataforge.tables.*;
import hep.dataforge.values.Values;
import inr.numass.NumassUtils;
import inr.numass.data.api.NumassAnalyzer;
import inr.numass.data.api.NumassPoint;

import java.util.*;

/**
 * @author Darksnake
 */
@TypedActionDef(name = "numass.merge", inputType = Table.class, outputType = Table.class, info = "Merge different numass data files into one.")
@NodeDef(name = "grouping", info = "The definition of grouping rule for this merge", from = "method::hep.dataforge.actions.GroupBuilder.byMeta")
public class MergeDataAction extends ManyToOneAction<Table, Table> {

    public static final String MERGE_NAME = "mergeName";
    public static String[] parnames = {NumassPoint.HV_KEY, NumassPoint.LENGTH_KEY, NumassAnalyzer.COUNT_KEY, NumassAnalyzer.COUNT_RATE_KEY, NumassAnalyzer.COUNT_RATE_ERROR_KEY};

    @Override
    protected List<DataNode<Table>> buildGroups(Context context, DataNode<Table> input, Meta actionMeta) {
        Meta meta = inputMeta(context, input.meta(), actionMeta);
        List<DataNode<Table>> groups;
        if (meta.hasValue("grouping.byValue")) {
            groups = super.buildGroups(context, input, actionMeta);
        } else {
            groups = GroupBuilder.byValue(MERGE_NAME, meta.getString(MERGE_NAME, input.getName())).group(input);
        }
        return groups;
    }

    @Override
    protected Table execute(Context context, String nodeName, Map<String, Table> data, Laminate meta) {
        Table res = mergeDataSets(data.values());
        return new ListTable(res.getFormat(), TableTransform.sort(res, NumassPoint.HV_KEY, true));
    }

    @Override
    protected void afterGroup(Context context, String groupName, Meta outputMeta, Table output) {
        output(context, groupName, stream -> NumassUtils.INSTANCE.write(stream, outputMeta, output));
    }

    private Values mergeDataPoints(Values dp1, Values dp2) {
        if (dp1 == null) {
            return dp2;
        }
        if (dp2 == null) {
            return dp1;
        }

        double voltage = dp1.getValue(NumassPoint.HV_KEY).doubleValue();
        double t1 = dp1.getValue(NumassPoint.LENGTH_KEY).doubleValue();
        double t2 = dp2.getValue(NumassPoint.LENGTH_KEY).doubleValue();
        double time = t1 + t2;

        long total = dp1.getValue(NumassAnalyzer.COUNT_KEY).intValue() + dp2.getValue(NumassAnalyzer.COUNT_KEY).intValue();

        double cr1 = dp1.getValue(NumassAnalyzer.COUNT_RATE_KEY).doubleValue();
        double cr2 = dp2.getValue(NumassAnalyzer.COUNT_RATE_KEY).doubleValue();

        double cr = (cr1 * t1 + cr2 * t2) / (t1 + t2);

        double err1 = dp1.getDouble(NumassAnalyzer.COUNT_RATE_ERROR_KEY);
        double err2 = dp2.getDouble(NumassAnalyzer.COUNT_RATE_ERROR_KEY);

        // абсолютные ошибки складываются квадратично
        double crErr = Math.sqrt(err1 * err1 * t1 * t1 + err2 * err2 * t2 * t2) / time;

        ValueMap.Builder map = ValueMap.of(parnames, voltage, time, total, cr, crErr).builder();

        return map.build();
    }

    private Table mergeDataSets(Collection<Table> ds) {
        //Сливаем все точки в один набор данных
        Map<Double, List<Values>> points = new LinkedHashMap<>();
        for (Table d : ds) {
            if (!d.getFormat().getNames().contains(parnames)) {
                throw new IllegalArgumentException();
            }
            for (Values dp : d) {
                double uset = dp.getValue(NumassPoint.HV_KEY).doubleValue();
                if (!points.containsKey(uset)) {
                    points.put(uset, new ArrayList<>());
                }
                points.get(uset).add(dp);
            }
        }

        List<Values> res = new ArrayList<>();

        points.entrySet().stream().map((entry) -> {
            Values curPoint = null;
            for (Values newPoint : entry.getValue()) {
                curPoint = mergeDataPoints(curPoint, newPoint);
            }
            return curPoint;
        }).forEach(res::add);

        return new ListTable(MetaTableFormat.forNames(parnames), res);

    }

}
