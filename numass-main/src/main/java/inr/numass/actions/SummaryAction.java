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
import hep.dataforge.stat.fit.FitState;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.io.ColumnedDataWriter;
import hep.dataforge.io.reports.Reportable;
import hep.dataforge.meta.Meta;
import hep.dataforge.tables.DataPoint;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.MapPoint;
import hep.dataforge.tables.Table;
import hep.dataforge.tables.TableFormat;
import hep.dataforge.values.Value;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Darksnake
 */
@TypedActionDef(name = "summary", inputType = FitState.class, outputType = Table.class, info = "Generate summary for fit results of different datasets.")
public class SummaryAction extends ManyToOneAction<FitState, Table> {

    public static final String SUMMARY_NAME = "sumName";

    @Override
    @SuppressWarnings("unchecked")
    protected List<DataNode<Table>> buildGroups(DataNode input, Meta actionMeta) {
        Meta meta = inputMeta(input.meta(), actionMeta);
        List<DataNode<Table>> groups;
        if (meta.hasValue("grouping.byValue")) {
            groups = super.buildGroups(input, actionMeta);
        } else {
            groups = GroupBuilder.byValue(SUMMARY_NAME, meta.getString(SUMMARY_NAME, "summary")).group(input);
        }
        return groups;
    }

    @Override
    protected Table execute(Reportable log, String nodeName, Map<String, FitState> input, Meta meta) {
        String[] parNames = meta.getStringArray("parnames");
        String[] names = new String[2 * parNames.length + 2];
        names[0] = "file";
        for (int i = 0; i < parNames.length; i++) {
            names[2 * i + 1] = parNames[i];
            names[2 * i + 2] = parNames[i] + "Err";
        }
        names[names.length - 1] = "chi2";

        ListTable.Builder res = new ListTable.Builder(TableFormat.fixedWidth(8, names));

        double[] weights = new double[parNames.length];
        Arrays.fill(weights, 0);
        double[] av = new double[parNames.length];
        Arrays.fill(av, 0);

        input.forEach((String key, FitState value) -> {
            FitState state = value;
            Value[] values = new Value[names.length];
            values[0] = Value.of(key);
            for (int i = 0; i < parNames.length; i++) {
                Value val = Value.of(state.getParameters().getDouble(parNames[i]));
                values[2 * i + 1] = val;
                Value err = Value.of(state.getParameters().getError(parNames[i]));
                values[2 * i + 2] = err;
                double weight = 1 / err.doubleValue() / err.doubleValue();
                av[i] += val.doubleValue() * weight;
                weights[i] += weight;
            }
            values[values.length - 1] = Value.of(state.getChi2());
            DataPoint point = new MapPoint(names, values);
            res.row(point);
        });

        Value[] averageValues = new Value[names.length];
        averageValues[0] = Value.of("average");
        averageValues[averageValues.length - 1] = Value.of(0);

        for (int i = 0; i < parNames.length; i++) {
            averageValues[2 * i + 1] = Value.of(av[i] / weights[i]);
            averageValues[2 * i + 2] = Value.of(1 / Math.sqrt(weights[i]));
        }

        res.row(new MapPoint(names, averageValues));

        return res.build();
    }

    @Override
    protected void afterGroup(Reportable log, String groupName, Meta outputMeta, Table output) {
        OutputStream stream = buildActionOutput(groupName);
        ColumnedDataWriter.writeDataSet(stream, output, groupName);

        super.afterGroup(log, groupName, outputMeta, output);
    }

}
