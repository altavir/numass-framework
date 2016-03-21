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

import hep.dataforge.actions.ManyToOneAction;
import hep.dataforge.actions.GroupBuilder;
import hep.dataforge.context.Context;
import hep.dataforge.data.Data;
import hep.dataforge.data.DataNode;
import hep.dataforge.points.Format;
import hep.dataforge.points.DataPoint;
import hep.dataforge.points.ListPointSet;
import hep.dataforge.points.MapPoint;
import hep.dataforge.datafitter.FitState;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.io.ColumnedDataWriter;
import hep.dataforge.io.log.Logable;
import hep.dataforge.meta.Meta;
import hep.dataforge.values.Value;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import hep.dataforge.points.PointSet;
import java.util.function.Consumer;
import javafx.util.Pair;

/**
 *
 * @author Darksnake
 */
@TypedActionDef(name = "summary", inputType = FitState.class, outputType = PointSet.class, description = "Generate summary for fit results of different datasets.")
public class SummaryAction extends ManyToOneAction<FitState, PointSet> {

    public static final String SUMMARY_NAME = "sumName";

    public SummaryAction(Context context, Meta annotation) {
        super(context, annotation);
    }

    @Override
    @SuppressWarnings("unchecked")    
    protected List<DataNode<PointSet>> buildGroups(DataNode input) {
        Meta meta = inputMeta(input.meta());
        List<DataNode<PointSet>> groups;
        if (meta.hasValue("grouping.byValue")) {
            groups = super.buildGroups(input);
        } else {
            groups = GroupBuilder.byValue(SUMMARY_NAME, meta.getString(SUMMARY_NAME, "summary")).group(input);
        }
        return groups;
    }

    @Override
    protected PointSet execute(Logable log, DataNode<FitState> input) {
        String[] parNames = meta().getStringArray("parnames");
        String[] names = new String[2 * parNames.length + 2];
        names[0] = "file";
        for (int i = 0; i < parNames.length; i++) {
            names[2 * i + 1] = parNames[i];
            names[2 * i + 2] = parNames[i] + "Err";
        }
        names[names.length - 1] = "chi2";

        ListPointSet res = new ListPointSet(Format.forNames(8, names));

        double[] weights = new double[parNames.length];
        Arrays.fill(weights, 0);
        double[] av = new double[parNames.length];
        Arrays.fill(av, 0);

        input.stream().forEach(new Consumer<Pair<String, Data<? extends FitState>>>() {
            @Override
            public void accept(Pair<String, Data<? extends FitState>> item) {
                FitState state = item.getValue().get();
                Value[] values = new Value[names.length];
                values[0] = Value.of(item.getKey());
                for (int i = 0; i < parNames.length; i++) {
                    Value val = Value.of(state.getParameters().getValue(parNames[i]));
                    values[2 * i + 1] = val;
                    Value err = Value.of(state.getParameters().getError(parNames[i]));
                    values[2 * i + 2] = err;
                    double weight = 1 / err.doubleValue() / err.doubleValue();
                    av[i] += val.doubleValue() * weight;
                    weights[i] += weight;
                }
                values[values.length - 1] = Value.of(state.getChi2());
                DataPoint point = new MapPoint(names, values);
                res.add(point);
            }
        });

        Value[] averageValues = new Value[names.length];
        averageValues[0] = Value.of("average");
        averageValues[averageValues.length - 1] = Value.of(0);

        for (int i = 0; i < parNames.length; i++) {
            averageValues[2 * i + 1] = Value.of(av[i] / weights[i]);
            averageValues[2 * i + 2] = Value.of(1 / Math.sqrt(weights[i]));
        }

        res.add(new MapPoint(names, averageValues));

        return res;
    }

    @Override
    protected void afterGroup(Logable log, String groupName, Meta outputMeta, PointSet output) {
        OutputStream stream = buildActionOutput(groupName);
        ColumnedDataWriter.writeDataSet(stream, output, groupName);

        super.afterGroup(log, groupName, outputMeta, output);
    }

}
