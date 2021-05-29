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

import hep.dataforge.data.DataNode
import hep.dataforge.meta.Meta
import hep.dataforge.stat.fit.FitState
import hep.dataforge.tables.Table
import hep.dataforge.workspace.tasks.AbstractTask
import hep.dataforge.workspace.tasks.TaskModel
import inr.numass.actions.SummaryAction

/**
 * Created by darksnake on 16-Sep-16.
 */
object NumassFitSummaryTask : AbstractTask<Table>(Table::class.java) {
    override val name: String = "summary"

    override fun run(model: TaskModel, data: DataNode<Any>): DataNode<Table> {
        val actionMeta = model.meta.getMeta("summary")
        val checkedData = data.getCheckedNode("fit", FitState::class.java)
        return SummaryAction.run(model.context, checkedData, actionMeta)
    }

    override fun buildModel(model: TaskModel.Builder, meta: Meta) {
        model.dependsOn("fit", meta, "fit")
    }
}
