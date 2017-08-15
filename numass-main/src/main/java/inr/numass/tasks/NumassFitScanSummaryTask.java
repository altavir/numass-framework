/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.tasks;

import hep.dataforge.actions.Action;
import hep.dataforge.actions.ManyToOneAction;
import hep.dataforge.context.Context;
import hep.dataforge.data.DataNode;
import hep.dataforge.data.DataSet;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.meta.Laminate;
import hep.dataforge.meta.Meta;
import hep.dataforge.stat.fit.FitResult;
import hep.dataforge.stat.fit.ParamSet;
import hep.dataforge.stat.fit.UpperLimitGenerator;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.Table;
import hep.dataforge.tables.TableTransform;
import hep.dataforge.workspace.AbstractTask;
import hep.dataforge.workspace.TaskModel;
import inr.numass.utils.NumassUtils;

import java.util.Map;

/**
 * @author Alexander Nozik
 */
public class NumassFitScanSummaryTask extends AbstractTask<Table> {

    @Override
    protected DataNode<Table> run(TaskModel model, DataNode<?> data) {
        DataSet.Builder<Table> builder = DataSet.builder(Table.class);
        Action<FitResult, Table> action = new FitSummaryAction();
        DataNode<FitResult> input = data.getCheckedNode("fitscan", FitResult.class);
        input.nodeStream().filter(it -> it.dataSize(false) > 0).forEach(node ->
                builder.putData(node.getName(), action.run(model.getContext(), node, model.meta()).getData())
        );
        return builder.build();
    }

    @Override
    protected void updateModel(TaskModel.Builder model, Meta meta) {
        model.dependsOn("fitscan", meta, "fitscan");
    }


    @Override
    public String getName() {
        return "scansum";
    }

    @TypedActionDef(name = "sterileSummary", inputType = FitResult.class, outputType = Table.class)
    private class FitSummaryAction extends ManyToOneAction<FitResult, Table> {

        @Override
        protected Table execute(Context context, String nodeName, Map<String, FitResult> input, Laminate meta) {
            ListTable.Builder builder = new ListTable.Builder("m", "U2", "U2err", "U2limit", "E0", "trap");
            input.forEach((key, fitRes) -> {
                ParamSet pars = fitRes.getParameters();

                double u2Val = pars.getDouble("U2") / pars.getError("U2");

                double limit;
                if (Math.abs(u2Val) < 3) {
                    limit = UpperLimitGenerator.getConfidenceLimit(u2Val) * pars.getError("U2");
                } else {
                    limit = Double.NaN;
                }

                builder.row(
                        Math.sqrt(pars.getValue("msterile2").doubleValue()),
                        pars.getValue("U2"),
                        pars.getError("U2"),
                        limit,
                        pars.getValue("E0"),
                        pars.getValue("trap"));
            });
            Table res = TableTransform.sort(builder.build(), "m", true);
            output(context, nodeName, stream -> NumassUtils.writeSomething(stream,meta,res));
            return res;
        }

    }

}
