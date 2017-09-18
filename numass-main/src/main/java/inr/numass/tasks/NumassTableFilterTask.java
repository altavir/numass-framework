package inr.numass.tasks;

import hep.dataforge.actions.Action;
import hep.dataforge.actions.OneToOneAction;
import hep.dataforge.context.Context;
import hep.dataforge.data.DataNode;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.meta.Laminate;
import hep.dataforge.meta.Meta;
import hep.dataforge.tables.Table;
import hep.dataforge.tables.TableTransform;
import hep.dataforge.values.Value;
import hep.dataforge.values.Values;
import hep.dataforge.workspace.tasks.SingleActionTask;
import hep.dataforge.workspace.tasks.TaskModel;
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
    protected void buildModel(TaskModel.Builder model, Meta meta) {
        if (meta.hasMeta("empty")) {
            model.dependsOn("subtractEmpty", meta, "prepare");
        } else {
            model.dependsOn("prepare", meta, "prepare");
        }
    }

    @Override
    protected Action<Table, Table> getAction(TaskModel model) {
        return new FilterTableAction();
    }

    @TypedActionDef(name = "filterTable", inputType = Table.class, outputType = Table.class)
    private class FilterTableAction extends OneToOneAction<Table, Table> {
        @Override
        protected Table execute(Context context, String name, Table input, Laminate inputMeta) {
            if (inputMeta.hasValue("from") || inputMeta.hasValue("to")) {
                double uLo = inputMeta.getDouble("from", 0);
                double uHi = inputMeta.getDouble("to", Double.POSITIVE_INFINITY);
                getLogger(context,inputMeta).debug("Filtering finished");
                return TableTransform.filter(input, "Uset", uLo, uHi);
            } else if (inputMeta.hasValue("condition")) {
                Predicate<Values> predicate = (dp) -> ExpressionUtils.condition(inputMeta.getString("condition"), unbox(dp));
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
            switch (val.getType()) {
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
