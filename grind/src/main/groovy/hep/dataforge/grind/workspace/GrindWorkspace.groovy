package hep.dataforge.grind.workspace

import groovy.transform.CompileStatic
import hep.dataforge.context.Context
import hep.dataforge.data.DataNode
import hep.dataforge.meta.Meta
import hep.dataforge.workspace.Workspace
import hep.dataforge.workspace.tasks.Task
import org.jetbrains.annotations.NotNull

/**
 * Workspace wrapper that implements methodMissing for tasks and propertyMissing for targets
 */
@CompileStatic
class GrindWorkspace implements Workspace {

    private Workspace workspace

    GrindWorkspace(Workspace workspace) {
        this.workspace = workspace
    }



    @Override
    DataNode<?> getData() {
        return workspace.getData()
    }

    @Override
    Collection<Task<?>> getTasks() {
        return workspace.tasks
    }

    @Override
    Collection<Meta> getTargets() {
        return workspace.targets
    }

    @Override
    Task<?> optTask(@NotNull String taskName) {
        return workspace.optTask(taskName)
    }

    @Override
    Meta optTarget(@NotNull String name) {
        return workspace.optTarget(name)
    }

    @Override
    void clean() {
        workspace.clean()
    }

    @Override
    Context getContext() {
        return workspace.context
    }

    def methodMissing(String name, Object args) {
        String str = args.getClass().isArray() ? ((Object[]) args).join(" ") : args.toString()
        return runTask(name, str)
    }

    def propertyMissing(String name) {
        return getTarget(name)
    }
}
