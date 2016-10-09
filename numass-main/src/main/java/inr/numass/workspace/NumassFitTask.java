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

package inr.numass.workspace;

import hep.dataforge.actions.Action;
import hep.dataforge.data.DataNode;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.stat.fit.FitAction;
import hep.dataforge.stat.fit.FitState;
import hep.dataforge.tables.Table;
import hep.dataforge.workspace.SingleActionTask;
import hep.dataforge.workspace.TaskModel;

/**
 * Created by darksnake on 16-Sep-16.
 */
public class NumassFitTask extends SingleActionTask<Table, FitState> {

    @Override
    public String getName() {
        return "numass.fit";
    }

    @Override
    protected DataNode<Table> gatherNode(DataNode<?> data) {
        return data.getCheckedNode("prepare", Table.class);
    }

    @Override
    public void validate(TaskModel model) {
        if (!model.meta().hasMeta("fit")) {
            throw new RuntimeException("Fit element not found in model");
        }
    }

    @Override
    protected Action getAction(TaskModel model) {
        return new FitAction().withContext(model.getContext());
    }

    @Override
    protected Meta transformMeta(TaskModel model) {
        return model.meta().getMeta("fit");
    }

    @Override
    protected TaskModel transformModel(TaskModel model) {
        //Transmit meta as-is
        MetaBuilder metaBuilder = new MetaBuilder(model.meta()).removeNode("fit");
        if (model.meta().hasMeta("filter")) {
            model.dependsOn("numass.filter", metaBuilder.build(), "prepare");
        } else {
            model.dependsOn("numass.prepare", metaBuilder.build(), "prepare");
        }
        return model;
    }
}
