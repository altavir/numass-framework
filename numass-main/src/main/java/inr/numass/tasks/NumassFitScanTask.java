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
import hep.dataforge.data.DataTree;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
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
        Value scanValues = config.getValue("scanValues", Value.of("[2.5e5, 1e6, 2.25e6, 4e6, 6.25e6, 9e6]"));
        Action<Table, FitState> action = new FitAction().withContext(context).withParentProcess(callback.workName());
        DataTree.Builder<FitState> resultBuilder = DataTree.builder(FitState.class);
        DataNode<?> sourceNode = state.getData().getNode("prepare").get();

        if (config.hasNode("merge")) {
            //use merged data and ignore raw data
            sourceNode = sourceNode.getNode("merge").get();
        }

        //do fit

        Meta fitConfig = config.getMeta("fit");
        sourceNode.forEachDataWithType(Table.class, data -> {
            for (int i = 0; i < scanValues.listValue().size(); i++) {
                Value val = scanValues.listValue().get(i);
                MetaBuilder overrideMeta = new MetaBuilder(fitConfig);
                overrideMeta.setValue("@resultName", String.format("%s[%s=%s]", data.getName(), scanParameter, val.stringValue()));

                overrideMeta.getNodes("params.param").stream()
                        .filter(par -> par.getString("name") == scanParameter).forEach(par -> par.setValue("value", val));
//                Data<Table> newData = new Data<Table>(data.getGoal(),data.type(),overrideMeta);
                DataNode node = action.run(DataNode.of("fit_" + i, data, Meta.empty()), overrideMeta);
                resultBuilder.putData(data.getName() + ".fit_" + i, node.getData());
            }
        });


        state.finish(resultBuilder.build());
    }

    @Override
    protected TaskModel transformModel(TaskModel model) {
        //Transmit meta as-is
        MetaBuilder metaBuilder = new MetaBuilder(model.meta()).removeNode("fit");
        if (model.meta().hasNode("filter")) {
            model.dependsOn("numass.filter", metaBuilder.build(), "prepare");
        } else {
            model.dependsOn("numass.prepare", metaBuilder.build(), "prepare");
        }
        return model;
    }

    @Override
    public String getName() {
        return "numass.fitscan";
    }

}
