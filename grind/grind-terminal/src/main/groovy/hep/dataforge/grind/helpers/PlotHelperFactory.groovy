package hep.dataforge.grind.helpers

import hep.dataforge.context.Context
import hep.dataforge.meta.Meta

class PlotHelperFactory implements GrindHelperFactory {
    @Override
    GrindHelper build(Context context, Meta meta) {
        return new PlotHelper(context);
    }

    @Override
    String getName() {
        return "plots"
    }
}
