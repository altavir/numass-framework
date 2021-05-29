package hep.dataforge.workspace

import hep.dataforge.context.Context
import hep.dataforge.data.DataNode
import hep.dataforge.meta.Meta
import hep.dataforge.workspace.tasks.Task
import hep.dataforge.workspace.tasks.TaskModel

/**
 * A dynamic workspace can update workspace specification dynamically from external source. It fully delegates all tasks to loaded workspace.
 * Loading new workspace during calculations do not affect current progress because backing workspace is not affected by update.
 */
abstract class DynamicWorkspace : Workspace {

    /**
     * Check if backing workspace is loaded
     *
     * @return
     */
    protected var isValid: Boolean = false
        private set

    private lateinit var _workspace: Workspace

    /**
     * Get backing workspace instance
     *
     * @return
     */
    protected open val workspace: Workspace
        get() {
            if (!isValid) {
                _workspace = buildWorkspace()
                isValid = true
            }
            return _workspace
        }

    override val data: DataNode<*>
        get() = workspace.data

    override val tasks: Collection<Task<*>>
        get() = workspace.tasks

    override val targets: Collection<Meta>
        get() = workspace.targets

    override val context: Context
        get() = workspace.context

    /**
     * Build new workspace instance
     *
     * @return
     */
    protected abstract fun buildWorkspace(): Workspace

    /**
     * Invalidate current backing workspace
     */
    protected fun invalidate() {
        isValid = false
    }

    override fun optTask(taskName: String): Task<*>? {
        return workspace.optTask(taskName)
    }

    override fun optTarget(name: String): Meta? {
        return workspace.optTarget(name)
    }

    override fun clean() {
        workspace.clean()
    }

    override fun runTask(model: TaskModel): DataNode<*> {
        return workspace.runTask(model)
    }
}
