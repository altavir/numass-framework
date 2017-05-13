package inr.numass.control

import javafx.scene.layout.VBox
import tornadofx.*

/**
 * Created by darksnake on 11-May-17.
 */
class BoardView : View("Numass control board") {
    private var deviceList: VBox by singleAssign();
    private val controller: BoardController by inject();

    override val root = borderpane {
        center {
            deviceList = vbox {
                bindChildren(controller.devices) { DeviceInfoView(it).root }
            }

        }
    }

}
