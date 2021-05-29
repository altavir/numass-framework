package hep.dataforge.workspace.tasks

import hep.dataforge.actions.KPipe
import hep.dataforge.context.Context
import hep.dataforge.data.DataNode
import hep.dataforge.meta.Meta

abstract class PipeTask<T : Any, R : Any> protected constructor(final override val name: String, private val inputType: Class<T>, outputType: Class<R>) : AbstractTask<R>(outputType) {

    private val action = KPipe<T, R>(this.name, inputType, outputType) {
        result { input ->
            result(context, name, input, meta)
        }
    }


    override fun run(model: TaskModel, data: DataNode<Any>): DataNode<R> {
        return action.run(model.context, data.checked(inputType), model.meta)
    }

    abstract override fun buildModel(model: TaskModel.Builder, meta: Meta)

    protected abstract fun result(context: Context, name: String, input: T, meta: Meta): R
}
