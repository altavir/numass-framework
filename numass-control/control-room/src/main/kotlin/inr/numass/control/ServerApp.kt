package inr.numass.control

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import javafx.stage.Stage
import tornadofx.*

/**
 * Created by darksnake on 19-May-17.
 */
class ServerApp : App(BoardView::class) {
    val controller: BoardController by inject();
    var context: Context by singleAssign();

    override fun start(stage: Stage) {
        NumassControlUtils.getConfig(this).ifPresent {
            context = Global.getContext("NUMASS-SERVER");
            controller.load(context, it);
        }
        super.start(stage)
        NumassControlUtils.setDFStageIcon(stage)
    }

    override fun stop() {
        controller.devices.forEach {
            it.device.shutdown()
        }
        super.stop()
        context.close();
    }
}