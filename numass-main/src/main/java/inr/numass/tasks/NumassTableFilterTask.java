package inr.numass.tasks;

import hep.dataforge.actions.Action;
import hep.dataforge.actions.OneToOneAction;
import hep.dataforge.context.Context;
import hep.dataforge.data.DataNode;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.meta.Laminate;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.tables.Table;
import hep.dataforge.tables.TableTransform;
import hep.dataforge.values.Value;
import hep.dataforge.values.Values;
import hep.dataforge.workspace.SingleActionTask;
import hep.dataforge.workspace.TaskModel;
import inr.numass.utils.ExpressionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Created by darksnake on 13-Aug-16.
 */
public class NumassTableFilterTask extends SingleActionTask<Table, Table> {

    @Override
    public String getName() {
        return "filter";
    }

    @Override
    protected DataNode<Table> gatherNode(DataNode<?> data) {
        return data.getCheckedNode("prepare", Table.class);
    }

    @Override
    protected TaskModel transformModel(TaskModel model) {
        MetaBuilder metaBuilder = new MetaBuilder(model.meta()).removeNode("filter");
        if (model.meta().hasMeta("empty")) {
            model.dependsOn("substractEmpty", metaBuilder.build(), "prepare");
        } else {
            model.dependsOn("prepare", metaBuilder.build(), "prepare");
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
        protected Table execute(Context context, String name, Table input, Laminate inputMeta) {
            if (inputMeta.hasValue("filter.from") || inputMeta.hasValue("filter.to")) {
                double uLo = inputMeta.getDouble("filter.from", 0);
                double uHi = inputMeta.getDouble("filter.to", Double.POSITIVE_INFINITY);
                getLogger(inputMeta).debug("Filtering finished");
                return TableTransform.filter(input, "Uset", uLo, uHi);
            } else if (inputMeta.hasValue("filter.condition")) {
                Predicate<Values> predicate = (dp) -> ExpressionUtils.condition(inputMeta.getString("filter.condition"), unbox(dp));
                return TableTransform.filter(input, predicate);
            } else {
                throw new RuntimeException("No filtering condition specified");
            }
        }
    }

    private Map<String, Object> unbox(Values dp) {
        Map<String, Object> res = new HashMap<>();
        for (String field : dp.getNames()) {
            Value val = dp.getValue(field);
            Object obj;
            switch (val.valueType()) {
                case BOOLEAN:
                    obj = val.booleanValue();
                    break;
                case NUMBER:
                    obj = val.doubleValue();
                    break;
                case STRING:
                    obj = val.stringValue();
                    break;
                case TIME:
                    obj = val.timeValue();
                    break;
                case NULL:
                    obj = null;
                    break;
                default:
                    throw new Error("unreachable statement");
            }
            res.put(field, obj);
        }
        return res;
    }
}
