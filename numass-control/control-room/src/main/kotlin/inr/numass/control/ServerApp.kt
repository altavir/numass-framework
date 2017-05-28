package inr.numass.control

import hep.dataforge.context.Context
import hep.dataforge.context.Global
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
            val libDir = File(parameters.named.getOrDefault("libPath", "../lib"));
            val contextBuilder = Context
                    .builder("NUMASS-SERVER");
            if (libDir.exists()) {
                Global.logger().info("Found library directory {}. Loading it into server context", libDir)
                contextBuilder.classPath(libDir.listFiles { _, name -> name.endsWith(".jar") }.map { it.toURI().toURL() })
            }
            context = contextBuilder.build();
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