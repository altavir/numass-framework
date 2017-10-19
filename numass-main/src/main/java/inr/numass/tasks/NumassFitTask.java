/*
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package inr.numass.tasks;

import hep.dataforge.actions.Action;
import hep.dataforge.actions.ActionUtils;
import hep.dataforge.data.DataNode;
import hep.dataforge.meta.Meta;
import hep.dataforge.plotfit.PlotFitResultAction;
import hep.dataforge.stat.fit.FitAction;
import hep.dataforge.stat.fit.FitResult;
import hep.dataforge.tables.Table;
import hep.dataforge.workspace.tasks.SingleActionTask;
import hep.dataforge.workspace.tasks.TaskModel;

/**
 * Created by darksnake on 16-Sep-16.
 */
public class NumassFitTask extends SingleActionTask<Table, FitResult> {

    @Override
    public String getName() {
        return "fit";
    }

    @Override
    protected DataNode<Table> gatherNode(DataNode<?> data) {
        return data.getCheckedNode("prepare", Table.class);
    }

    @Override
    public void validate(TaskModel model) {
        if (model.meta().isEmpty()) {
            throw new RuntimeException("Fit element not found in model");
        }
    }

    @Override
    protected Action<Table, FitResult> getAction(TaskModel model) {
        Action<Table, FitResult> action = new FitAction();
        if (model.meta().getBoolean("frame", false)) {
            return ActionUtils.compose(action, new PlotFitResultAction());
        } else {
            return action;
        }
    }

    @Override
    protected Meta transformMeta(TaskModel model) {
        return model.meta();
    }


    @Override
    protected void buildModel(TaskModel.Builder model, Meta meta) {
        if (meta.hasMeta("filter")) {
            model.dependsOn("filter", meta, "prepare");
        } else if (meta.hasMeta("empty")) {
            model.dependsOn("subtractEmpty", meta, "prepare");
        } else {
            model.dependsOn("prepare", meta, "prepare");
        }
    }
}
