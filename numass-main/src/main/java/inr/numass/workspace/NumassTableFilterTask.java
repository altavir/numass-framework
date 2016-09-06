package inr.numass.workspace;

import hep.dataforge.actions.Action;
import hep.dataforge.actions.OneToOneAction;
import hep.dataforge.data.DataNode;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.meta.Laminate;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.tables.Table;
import hep.dataforge.workspace.SingleActionTask;
import hep.dataforge.workspace.TaskModel;

/**
 * Created by darksnake on 13-Aug-16.
 */
public class NumassTableFilterTask extends SingleActionTask<Table, Table> {

    @Override
    public String getName() {
        return "numass.filter";
    }

    @Override
    protected DataNode<Table> gatherNode(DataNode<?> data) {
        return data.getCheckedNode("prepare", Table.class);
    }

    @Override
    protected TaskModel transformModel(TaskModel model) {
        MetaBuilder metaBuilder = new MetaBuilder(model.meta()).removeNode("filter");
        if (model.meta().hasNode("empty")) {
            model.dependsOn("numass.substractEmpty", metaBuilder.build(), "prepare");
        } else {
            model.dependsOn("numass.prepare", metaBuilder.build(), "prepare");
        }
        return model;
    }

    @Override
    protected Action<Table, Table> getAction(TaskModel model) {
        return new FilterTableAction();
    }

    @TypedActionDef(name = "filterTable", inputType = Table.class, outputType = Table.class)
    private class FilterTableAction extends OneToOneAction<Table, Table> {
        @Override
        protected Table execute(String name, Laminate inputMeta, Table input) {
            double uLo = inputMeta.getDouble("filter.from", 0);
            double uHi = inputMeta.getDouble("filter.to", Double.POSITIVE_INFINITY);
            getLogger().debug("Filtering finished");
            return input.filter("Uset", uLo, uHi);
        }
    }
}
