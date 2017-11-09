package inr.numass.control

import javafx.stage.Stage
import tornadofx.*

/**
 * Created by darksnake on 19-May-17.
 */
class ServerApp : App(BoardView::class) {
    private val controller: BoardController by inject();


    override fun start(stage: Stage) {
        controller.load(this)
        super.start(stage)
    }

    override fun stop() {
        controller.close()
        super.stop()
    }
}