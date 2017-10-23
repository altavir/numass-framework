/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.tasks;

import hep.dataforge.actions.GenericAction;
import hep.dataforge.cache.CachePlugin;
import hep.dataforge.context.Context;
import hep.dataforge.data.DataNode;
import hep.dataforge.data.DataTree;
import hep.dataforge.description.NodeDef;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.meta.Template;
import hep.dataforge.tables.Table;
import hep.dataforge.workspace.tasks.AbstractTask;
import hep.dataforge.workspace.tasks.TaskModel;
import inr.numass.actions.AnalyzeDataAction;
import inr.numass.actions.MergeDataAction;
import inr.numass.actions.MonitorCorrectAction;
import inr.numass.actions.TransformDataAction;
import inr.numass.data.api.NumassSet;

/**
 * Prepare data task
 *
 * @author Alexander Nozik
 */
@NodeDef(name = "prepare")
@NodeDef(name = "monitor")
@NodeDef(name = "merge")
@Deprecated
public class NumassPrepareTask extends AbstractTask<Table> {

    @Override
    protected DataNode<Table> run(TaskModel model, DataNode<?> input) {
        Meta config = model.meta();
        Context context = model.getContext();

        //acquiring initial data. Data node could not be empty
        DataNode<NumassSet> data = input.getCheckedNode("data", NumassSet.class);

        //preparing table data
        Meta prepareMeta = config.getMeta("prepare");
        DataNode<Table> tables = runAction(new AnalyzeDataAction(), context, data, prepareMeta);

        tables = runAction(new TransformDataAction(), context, tables, prepareMeta);

        //intermediate caching
        tables = model.getContext().getFeature(CachePlugin.class).cacheNode("prepare", prepareMeta, tables);

        if (config.hasMeta("monitor")) {
            Meta monitorMeta = config.getMeta("monitor");
            tables = runAction(new MonitorCorrectAction(), context, tables, monitorMeta);
        }

        //merging if needed
        if (config.hasMeta("merge")) {
            DataTree.Builder<Table> resultBuilder = DataTree.builder(Table.class);
            DataTree.Builder<Table> tablesForMerge = new DataTree.Builder<>(tables);


            config.getMetaList("merge").forEach(mergeNode -> {
                Meta mergeMeta = Template.compileTemplate(mergeNode, config);
                DataNode<Table> mergeData = runAction(new MergeDataAction(), context, tablesForMerge.build(), mergeMeta);
                mergeData.dataStream().forEach(d -> {
                    resultBuilder.putData("merge." + d.getName(), d.anonymize());
                });
            });
            tables = resultBuilder.build();
        }

        return tables;
    }

    @Override
    protected void buildModel(TaskModel.Builder model, Meta meta) {
        model.configure(
                new MetaBuilder()
                        .putNode(meta.getMetaOrEmpty("prepare"))
                        .putNode(meta.getMetaOrEmpty("monitor"))
                        .putNode(meta.getMetaOrEmpty("merge"))

        );

        model.dependsOn("select", meta, "data");
    }

    private <T, R> DataNode<R> runAction(GenericAction<T, R> action, Context context, DataNode<T> data, Meta meta) {
        return action.run(context, data, meta);
    }

    @Override
    public String getName() {
        return "prepare";
    }

}
