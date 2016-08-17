package inr.numass.workspace;

import hep.dataforge.actions.OneToOneAction;
import hep.dataforge.computation.WorkManager;
import hep.dataforge.context.Context;
import hep.dataforge.data.DataNode;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.meta.Laminate;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.tables.Table;
import hep.dataforge.workspace.GenericTask;
import hep.dataforge.workspace.TaskModel;
import hep.dataforge.workspace.TaskState;

/**
 * Created by darksnake on 13-Aug-16.
 */
public class NumassTableFilterTask extends GenericTask<Table> {

    @Override
    public String getName() {
        return "numass.filter";
    }

    @Override
    protected void transform(WorkManager.Callback callback, Context context, TaskState state, Meta config) {
        DataNode<Table> sourceNode = (DataNode<Table>) state.getData().getNode("prepare").get();
        state.finish(new FilterTableAction().withContext(context).run(sourceNode, config));
    }

    @Override
    protected TaskModel transformModel(TaskModel model) {
        super.transformModel(model);
        MetaBuilder metaBuilder = new MetaBuilder(model.meta()).removeNode("filter");
        model.dependsOn("numass.prepare", metaBuilder.build(), "prepare");
        return model;
    }

    @TypedActionDef(name = "filterTable", inputType = Table.class, outputType = Table.class)
    private class FilterTableAction extends OneToOneAction<Table, Table> {
        @Override
        protected Table execute(String name, Laminate inputMeta, Table input) {
            double uLo = inputMeta.getDouble("filter.from", 0);
            double uHi = inputMeta.getDouble("filter.to", Double.POSITIVE_INFINITY);
            return input.filter("Uset", uLo, uHi);
        }
    }
}
