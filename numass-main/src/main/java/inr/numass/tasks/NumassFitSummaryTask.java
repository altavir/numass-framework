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
import hep.dataforge.data.DataNode;
import hep.dataforge.meta.Meta;
import hep.dataforge.stat.fit.FitState;
import hep.dataforge.tables.Table;
import hep.dataforge.workspace.tasks.SingleActionTask;
import hep.dataforge.workspace.tasks.TaskModel;
import inr.numass.actions.SummaryAction;

/**
 * Created by darksnake on 16-Sep-16.
 */
public class NumassFitSummaryTask extends SingleActionTask<FitState, Table> {
    @Override
    public String getName() {
        return "summary";
    }

    @Override
    protected Action<FitState, Table> getAction(TaskModel model) {
        return new SummaryAction();
    }

    @Override
    protected DataNode<FitState> gatherNode(DataNode<?> data) {
        return data.getCheckedNode("fit", FitState.class);
    }

    @Override
    protected Meta transformMeta(TaskModel model) {
        return model.meta().getMeta("summary");
    }

    @Override
    protected void buildModel(TaskModel.Builder model, Meta meta) {
        model.dependsOn("fit", meta, "fit");
    }
}
