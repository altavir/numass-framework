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

package inr.numass.tasks

import hep.dataforge.actions.Action
import hep.dataforge.actions.ActionUtils
import hep.dataforge.data.DataNode
import hep.dataforge.meta.Meta
import hep.dataforge.plotfit.PlotFitResultAction
import hep.dataforge.stat.fit.FitAction
import hep.dataforge.stat.fit.FitResult
import hep.dataforge.tables.Table
import hep.dataforge.workspace.tasks.SingleActionTask
import hep.dataforge.workspace.tasks.TaskModel

/**
 * Created by darksnake on 16-Sep-16.
 */
class NumassFitTask : SingleActionTask<Table, FitResult>() {

    override fun getName(): String {
        return "fit"
    }

    override fun gatherNode(data: DataNode<*>): DataNode<Table> {
        return data.checked(Table::class.java)
    }

    override fun validate(model: TaskModel) {
        if (model.meta().isEmpty) {
            throw RuntimeException("Fit element not found in model")
        }
    }

    override fun getAction(model: TaskModel): Action<Table, FitResult> {
        val action = FitAction()
        return if (model.meta().getBoolean("frame", false)) {
            ActionUtils.compose(action, PlotFitResultAction())
        } else {
            action
        }
    }

    override fun buildModel(model: TaskModel.Builder, meta: Meta) {
        model.dependsOn("transform", meta);
    }
}
