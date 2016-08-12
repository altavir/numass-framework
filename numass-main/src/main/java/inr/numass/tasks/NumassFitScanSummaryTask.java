/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.tasks;

import hep.dataforge.actions.ManyToOneAction;
import hep.dataforge.computation.WorkManager;
import hep.dataforge.context.Context;
import hep.dataforge.data.DataNode;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.io.reports.Reportable;
import hep.dataforge.meta.Meta;
import hep.dataforge.stat.fit.FitState;
import hep.dataforge.stat.fit.ParamSet;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.Table;
import hep.dataforge.workspace.GenericTask;
import hep.dataforge.workspace.TaskModel;
import hep.dataforge.workspace.TaskState;

import java.util.Map;

/**
 *
 * @author Alexander Nozik
 */
public class NumassFitScanSummaryTask extends GenericTask {

    @Override
    protected void transform(WorkManager.Callback callback, Context context, TaskState state, Meta config) {
        state.finish(new FitSummaryAction().withContext(context).run((DataNode<FitState>) state.getData(), config));
    }

    @Override
    protected TaskModel transformModel(TaskModel model) {
        //Transmit meta as-is
        model.dependsOn("numass.fitscan", model.meta());
        return model;
    }

    @Override
    public String getName() {
        return "numass.fitsum";
    }

    @TypedActionDef(name = "fitSummary", inputType = FitState.class, outputType = Table.class)
    private class FitSummaryAction extends ManyToOneAction<FitState, Table> {

        @Override
        protected Table execute(Reportable log, String nodeName, Map<String, FitState> input, Meta meta) {
            ListTable.Builder builder = new ListTable.Builder("msterile2", "U2", "U2err", "E0", "trap");
            input.forEach((key, fitRes) -> {
                ParamSet pars = fitRes.getParameters();
                builder.row(pars.getValue("msterile2"),
                        pars.getValue("U2"),
                        pars.getError("U2"),
                        pars.getValue("E0"),
                        pars.getValue("trap"));
            });
            return builder.build();
        }

    }

}
