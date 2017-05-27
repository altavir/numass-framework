package inr.numass.control

import hep.dataforge.context.Context
import javafx.stage.Stage
import tornadofx.*
import java.io.File

/**
 * Created by darksnake on 19-May-17.
 */
class ServerApp : App(BoardView::class) {
    val controller: BoardController by inject();
    var context: Context by singleAssign();

    override fun start(stage: Stage) {
        NumassControlUtils.getConfig(this).ifPresent {
            val libPath = parameters.named.getOrDefault("libPath","../lib");
            context = Context
                    .builder("NUMASS-SERVER")
                    .classPath(File(libPath).toURI().toURL())
                    .build()
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