package hep.dataforge.grind.helpers

import hep.dataforge.context.Context
import hep.dataforge.meta.Meta

class WorkspaceHelperFactory implements GrindHelperFactory {
    @Override
    String getName() {
        return "spaces"
    }

    @Override
    GrindHelper build(Context context, Meta meta) {
        return new WorkspaceHelper(context)
    }
}
