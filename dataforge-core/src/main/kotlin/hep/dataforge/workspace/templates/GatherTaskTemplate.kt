package hep.dataforge.workspace.templates

import hep.dataforge.context.Context
import hep.dataforge.data.DataNode
import hep.dataforge.meta.Meta
import hep.dataforge.workspace.tasks.AbstractTask
import hep.dataforge.workspace.tasks.Task
import hep.dataforge.workspace.tasks.TaskModel

/**
 * The task that gathers data from workspace and returns it as is.
 * The task configuration is considered to be dependency configuration.
 */
class GatherTaskTemplate : TaskTemplate {
    override val name: String = "gather"

    override fun build(context: Context, meta: Meta): Task<*> {
        return object : AbstractTask<Any>(Any::class.java) {
            override fun run(model: TaskModel, data: DataNode<Any>): DataNode<Any> {
                return data
            }

            override fun buildModel(model: TaskModel.Builder, meta: Meta) {
                if (meta.hasMeta("data")) {
                    meta.getMetaList("data").forEach { dataElement ->
                        val dataPath = dataElement.getString("name")
                        model.data(dataPath, dataElement.getString("as", dataPath))
                    }
                }
                //Iterating over task dependancies
                if (meta.hasMeta("task")) {
                    meta.getMetaList("task").forEach { taskElement ->
                        val taskName = taskElement.getString("name")
                        val task = model.workspace.getTask(taskName)
                        //Building model with default data construction
                        model.dependsOn(task.build(model.workspace, taskElement), taskElement.getString("as", taskName))
                    }
                }
            }

            override val name: String =meta.getString("name", this@GatherTaskTemplate.name)
        }
    }
}
