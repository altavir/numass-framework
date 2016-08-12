/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.tasks;

import hep.dataforge.actions.Action;
import hep.dataforge.computation.WorkManager;
import hep.dataforge.context.Context;
import hep.dataforge.data.DataNode;
import hep.dataforge.data.DataSet;
import hep.dataforge.data.DataTree;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.meta.MetaUtils;
import hep.dataforge.stat.fit.FitAction;
import hep.dataforge.stat.fit.FitState;
import hep.dataforge.tables.Table;
import hep.dataforge.values.Value;
import hep.dataforge.workspace.GenericTask;
import hep.dataforge.workspace.TaskModel;
import hep.dataforge.workspace.TaskState;

/**
 * @author Alexander Nozik
 */
public class NumassFitScanTask extends GenericTask {

    @Override
    protected void transform(WorkManager.Callback callback, Context context, TaskState state, Meta config) {
        String scanParameter = config.getString("scanPar", "msterile2");
        Value scanValues = config.getValue("scanValues", Value.of(new String[]{"0.5, 1, 1.5, 2, 2.5, 3"}));
        Action action = new FitAction().withContext(context).withParentProcess(callback.workName());
        DataTree.Builder resultBuilder = DataTree.builder(FitState.class);
        DataNode<?> sourceNode = state.getData();

        if (config.hasNode("merge")) {
            //use merged data and ignore raw data
            sourceNode = sourceNode.getNode("merge").get();
        }

        //do fit
        sourceNode.forEachDataWithType(Table.class, data -> {
            DataNode res = scanValues.listValue().stream().parallel().map(val -> {
                MetaBuilder overrideMeta = new MetaBuilder("override");
                overrideMeta.setValue("@resultName", String.format("%s[%s=%s]", data.getName(), scanParameter, val.stringValue()));
                MetaBuilder paramMeta = MetaUtils.findNodeByValue(config, "params.param", data.getName(), scanParameter).getBuilder()
                        .setValue("value", val);
                overrideMeta.setNode("params.param", paramMeta);
                return action.run(DataNode.of(data.getName(), data, overrideMeta), config);
            }).collect(
                    () -> DataSet.builder(FitState.class),
                    (DataSet.Builder builder, DataNode node) -> builder.putData(node.getName(), node.getData()),
                    (DataSet.Builder builder1, DataSet.Builder builder2) -> builder1.putAll(builder2.getDataMap())
            ).build();
            resultBuilder.putData(data.getName(), res.getData());
        });


        state.finish(resultBuilder.build());
    }

    @Override
    protected TaskModel transformModel(TaskModel model) {
        //Transmit meta as-is
        model.dependsOn("numass.prepare", model.meta());
        return model;
    }

    @Override
    public String getName() {
        return "numass.fitscan";
    }

}
