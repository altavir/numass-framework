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
import hep.dataforge.meta.Meta;
import hep.dataforge.content.NamedGroup;
import hep.dataforge.content.GroupBuilder;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.context.Context;
import hep.dataforge.data.DataFormat;
import hep.dataforge.data.DataPoint;
import hep.dataforge.data.DataSet;
import hep.dataforge.data.ListDataSet;
import hep.dataforge.data.MapDataPoint;
import hep.dataforge.values.Value;
import hep.dataforge.datafitter.FitState;
import hep.dataforge.io.ColumnedDataWriter;
import hep.dataforge.io.log.Logable;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Darksnake
 */
@TypedActionDef(name = "summary", inputType = FitState.class, outputType = DataSet.class, description = "Generate summary for fit results of different datasets.")
public class SummaryAction extends ManyToOneAction<FitState, DataSet> {
    
    public static final String SUMMARY_NAME = "sumName";

    public SummaryAction(Context context, Meta annotation) {
        super(context, annotation);
    }
    
    @Override
    protected List<NamedGroup<FitState>> buildGroups(Meta reader, List<FitState> input) {
        List<NamedGroup<FitState>> groups;
        if (reader.hasNode("grouping")) {
            groups = super.buildGroups(reader, input);
        } else {
            groups = GroupBuilder.byValue(SUMMARY_NAME, reader.getString(SUMMARY_NAME, "summary")).group(input);
        }
        return groups;
    }    

    @Override
    protected DataSet execute(Logable log, Meta reader, NamedGroup<FitState> input){
        String[] parNames = meta().getStringArray("parnames");
        String[] names = new String[2 * parNames.length + 2];
        names[0] = "file";
        for (int i = 0; i < parNames.length; i++) {
            names[2 * i + 1] = parNames[i];
            names[2 * i + 2] = parNames[i] + "Err";
        }
        names[names.length - 1] = "chi2";

//        boolean calculateWAV = meta().getBoolean("wav", true);
        String fileName = reader.getString(SUMMARY_NAME, "summary");

        ListDataSet res = new ListDataSet(fileName, DataFormat.forNames(8, names));

        double[] weights = new double[parNames.length];
        Arrays.fill(weights, 0);
        double[] av = new double[parNames.length];
        Arrays.fill(av, 0);

        for (FitState state : input) {
            Value[] values = new Value[names.length];
            values[0] = Value.of(state.getName());
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
            DataPoint point = new MapDataPoint(names, values);
            res.add(point);
        }

        Value[] averageValues = new Value[names.length];
        averageValues[0] = Value.of("average");
        averageValues[averageValues.length - 1] = Value.of(0);

        for (int i = 0; i < parNames.length; i++) {
            averageValues[2 * i + 1] = Value.of(av[i] / weights[i]);
            averageValues[2 * i + 2] = Value.of(1 / Math.sqrt(weights[i]));
        }

        res.add(new MapDataPoint(names, averageValues));

        OutputStream stream = buildActionOutput(res);

        ColumnedDataWriter.writeDataSet(stream, res, fileName);

        return res;
    }

}
