package hep.dataforge.grind

import hep.dataforge.data.DataNodeBuilder
import hep.dataforge.data.DataSet
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaUtils
import hep.dataforge.workspace.tasks.MultiStageTask
import hep.dataforge.workspace.tasks.TaskModel

/**
 * Created by darksnake on 04-Aug-16.
 */
class TestTask extends MultiStageTask {
    TestTask() {
        super(Object)
    }

    @Override
    String getName() {
        return "testTask"
    }

    @Override
    protected MultiStageTask.MultiStageTaskState transform(TaskModel model, MultiStageTask.MultiStageTaskState state) {
        DataNodeBuilder b = DataSet.edit()
        model.context.getProperties().forEach { key, value ->
            b.putStatic(key, value);
        }
        MetaUtils.valueStream(model.getMeta()).forEach { pair ->
            b.putStatic("meta." + pair.first, pair.second)
        }

        state.finish(b.build())
    }

    @Override
    protected void buildModel(TaskModel.Builder model, Meta meta) {

    }
}
