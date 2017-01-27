/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.workspace;

import hep.dataforge.actions.Action;
import hep.dataforge.actions.ManyToOneAction;
import hep.dataforge.context.Context;
import hep.dataforge.data.DataNode;
import hep.dataforge.data.DataSet;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.io.ColumnedDataWriter;
import hep.dataforge.meta.Meta;
import hep.dataforge.stat.fit.FitState;
import hep.dataforge.stat.fit.ParamSet;
import hep.dataforge.stat.fit.UpperLimitGenerator;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.Table;
import hep.dataforge.tables.TableTransform;
import hep.dataforge.workspace.AbstractTask;
import hep.dataforge.workspace.TaskModel;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * @author Alexander Nozik
 */
public class NumassFitScanSummaryTask extends AbstractTask<Table> {

    @Override
    protected DataNode<Table> run(TaskModel model, DataNode<?> data) {
        DataSet.Builder<Table> builder = DataSet.builder(Table.class);
        Action<FitState, Table> action = new FitSummaryAction();
        DataNode<FitState> input = data.getCheckedNode("fitscan", FitState.class);
        input.nodeStream().filter(it -> it.dataSize(false) > 0).forEach(node ->
                builder.putData(node.getName(), action.run(model.getContext(), node, model.meta()).getData()));
        return builder.build();
    }

    @Override
    protected TaskModel transformModel(TaskModel model) {
        //Transmit meta as-is
        model.dependsOn("fitscan", model.meta(), "fitscan");
        return model;
    }

    @Override
    public String getName() {
        return "scansum";
    }

    @TypedActionDef(name = "sterileSummary", inputType = FitState.class, outputType = Table.class)
    private class FitSummaryAction extends ManyToOneAction<FitState, Table> {

        @Override
        protected Table execute(Context context, String nodeName, Map<String, FitState> input, Meta meta) {
            ListTable.Builder builder = new ListTable.Builder("msterile2", "U2", "U2err", "U2limit", "E0", "trap");
            input.forEach((key, fitRes) -> {
                ParamSet pars = fitRes.getParameters();

                double u2Val = pars.getDouble("U2") / pars.getError("U2");

                double limit;
                if (Math.abs(u2Val) < 3) {
                    limit = UpperLimitGenerator.getConfidenceLimit(u2Val) * pars.getError("U2");
                } else {
                    limit = Double.NaN;
                }

                builder.row(pars.getValue("msterile2"),
                        pars.getValue("U2"),
                        pars.getError("U2"),
                        limit,
                        pars.getValue("E0"),
                        pars.getValue("trap"));
            });
            Table res = TableTransform.sort(builder.build(), "msterile2", true);


            try (OutputStream stream = buildActionOutput(context, nodeName)) {
                String head = "Sterile neutrino mass scan summary\n" + meta.toString();
                ColumnedDataWriter.writeDataSet(stream, res, head);
            } catch (IOException e) {
                getLogger(meta).error("Failed to close output stream", e);
            }

            return res;
        }

    }

}
