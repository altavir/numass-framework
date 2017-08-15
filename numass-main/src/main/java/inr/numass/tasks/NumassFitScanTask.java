/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.tasks;

import hep.dataforge.actions.Action;
import hep.dataforge.data.DataNode;
import hep.dataforge.data.DataTree;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.stat.fit.FitAction;
import hep.dataforge.stat.fit.FitResult;
import hep.dataforge.tables.Table;
import hep.dataforge.values.Value;
import hep.dataforge.workspace.AbstractTask;
import hep.dataforge.workspace.TaskModel;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Alexander Nozik
 */
public class NumassFitScanTask extends AbstractTask<FitResult> {


    @Override
    protected DataNode<FitResult> run(TaskModel model, DataNode<?> data) {
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
        Action<Table, FitResult> action = new FitAction();
        DataTree.Builder<FitResult> resultBuilder = DataTree.builder(FitResult.class);
        DataNode<Table> sourceNode = data.getCheckedNode("prepare", Table.class);

        if (config.hasMeta("merge")) {
            //use merged data and ignore raw data
            sourceNode = sourceNode.getCheckedNode("merge", Table.class);
        }

        //do fit

        Meta fitConfig = config.getMeta("fit");
        sourceNode.dataStream().forEach(table -> {
            for (int i = 0; i < scanValues.listValue().size(); i++) {
                Value val = scanValues.listValue().get(i);
                MetaBuilder overrideMeta = new MetaBuilder(fitConfig);

                String resultName = String.format("%s[%s=%s]", table.getName(), scanParameter, val.stringValue());
//                overrideMeta.setValue("@resultName", String.format("%s[%s=%s]", table.getName(), scanParameter, val.stringValue()));

                if (overrideMeta.hasMeta("params." + scanParameter)) {
                    overrideMeta.setValue("params." + scanParameter + ".value", val);
                } else {
                    overrideMeta.getMetaList("params.param").stream()
                            .filter(par -> Objects.equals(par.getString("name"), scanParameter)).forEach(par -> par.setValue("value", val));
                }
//                Data<Table> newData = new Data<Table>(data.getGoal(),data.type(),overrideMeta);
                DataNode<FitResult> node = action.run(model.getContext(), DataNode.of(resultName, table, Meta.empty()), overrideMeta);
                resultBuilder.putData(table.getName() + ".fit_" + i, node.getData());
            }
        });


        return resultBuilder.build();
    }

    @Override
    protected void updateModel(TaskModel.Builder model, Meta meta) {
        if (meta.hasMeta("filter")) {
            model.dependsOn("filter", meta, "prepare");
        } else if (meta.hasMeta("empty")) {
            model.dependsOn("subtractEmpty", meta, "prepare");
        } else {
            model.dependsOn("prepare", meta, "prepare");
        }
    }

    @Override
    public String getName() {
        return "fitscan";
    }

}
