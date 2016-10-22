/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.workspace;

import hep.dataforge.actions.Action;
import hep.dataforge.computation.ProgressCallback;
import hep.dataforge.data.DataNode;
import hep.dataforge.data.DataTree;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.stat.fit.FitAction;
import hep.dataforge.stat.fit.FitState;
import hep.dataforge.tables.Table;
import hep.dataforge.values.Value;
import hep.dataforge.workspace.AbstractTask;
import hep.dataforge.workspace.TaskModel;

import java.util.stream.Collectors;

/**
 * @author Alexander Nozik
 */
public class NumassFitScanTask extends AbstractTask<FitState> {


    @Override
    protected DataNode<FitState> run(TaskModel model, ProgressCallback callback, DataNode<?> data) {
        Meta config = model.meta();
        String scanParameter = config.getString("scan.parameter", "msterile2");

        Value scanValues;
        if (config.hasValue("scan.masses")) {
            scanValues = Value.of(config.getValue("scan.masses")
                    .listValue().stream()
                    .map(it -> Math.pow(it.doubleValue() * 1000, 2.0))
                    .collect(Collectors.toList())
            );
        } else {
            scanValues = config.getValue("scan.values", Value.of("[2.5e5, 1e6, 2.25e6, 4e6, 6.25e6, 9e6]"));
        }
        Action<Table, FitState> action = new FitAction().withContext(model.getContext()).withParentProcess(callback.workName());
        DataTree.Builder<FitState> resultBuilder = DataTree.builder(FitState.class);
        DataNode<Table> sourceNode = data.getCheckedNode("prepare", Table.class);

        if (config.hasMeta("merge")) {
            //use merged data and ignore raw data
            sourceNode = sourceNode.getCheckedNode("merge", Table.class);
        }

        //do fit

        Meta fitConfig = config.getMeta("fit");
        sourceNode.forEachDataWithType(Table.class, d -> {
            for (int i = 0; i < scanValues.listValue().size(); i++) {
                Value val = scanValues.listValue().get(i);
                MetaBuilder overrideMeta = new MetaBuilder(fitConfig);
                overrideMeta.setValue("@resultName", String.format("%s[%s=%s]", d.getName(), scanParameter, val.stringValue()));

                if (overrideMeta.hasMeta("params." + scanParameter)) {
                    overrideMeta.setValue("params." + scanParameter + ".value", val);
                } else {
                    overrideMeta.getMetaList("params.param").stream()
                            .filter(par -> par.getString("name") == scanParameter).forEach(par -> par.setValue("value", val));
                }
//                Data<Table> newData = new Data<Table>(data.getGoal(),data.type(),overrideMeta);
                DataNode node = action.run(DataNode.of("fit_" + i, d, Meta.empty()), overrideMeta);
                resultBuilder.putData(d.getName() + ".fit_" + i, node.getData());
            }
        });


        return resultBuilder.build();
    }

    @Override
    protected TaskModel transformModel(TaskModel model) {
        //Transmit meta as-is
        MetaBuilder metaBuilder = new MetaBuilder(model.meta()).removeNode("fit").removeNode("scan");
        if (model.meta().hasMeta("filter")) {
            model.dependsOn("filter", metaBuilder.build(), "prepare");
        } else {
            model.dependsOn("prepare", metaBuilder.build(), "prepare");
        }
        return model;
    }

    @Override
    public String getName() {
        return "fitscan";
    }

}
