package hep.dataforge.grind.helpers

import groovy.transform.CompileStatic
import hep.dataforge.context.Context
import hep.dataforge.grind.workspace.WorkspaceSpec
import hep.dataforge.io.output.TextOutput
import hep.dataforge.meta.Meta
import org.jetbrains.annotations.NotNull

import java.lang.reflect.Method

@CompileStatic
class WorkspaceHelper extends AbstractHelper {
    @Delegate private WorkspaceSpec builder;

    WorkspaceHelper(Context context) {
        super(context)
        builder = new WorkspaceSpec(context);
    }

    @Override
    protected Collection<Method> listDescribedMethods() {
        return builder.getClass().getDeclaredMethods()
                .findAll { it.isAnnotationPresent(MethodDescription) }
    }

    @Override
    protected void renderDescription(@NotNull TextOutput output, @NotNull Meta meta) {
        output.renderText("The helper for workspace operations")
    }


}
