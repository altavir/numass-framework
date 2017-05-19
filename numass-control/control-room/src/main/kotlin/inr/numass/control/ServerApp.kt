package inr.numass.control

import hep.dataforge.context.Global
import javafx.scene.Scene
import tornadofx.*

/**
 * Created by darksnake on 19-May-17.
 */
class ServerApp : App(ServerView::class) {

    override fun createPrimaryScene(view: UIComponent): Scene {
        if (view is ServerView) {
            view.context = Global.getContext("NUMASS-SERVER")
            NumassControlUtils.getConfig(this).ifPresent { view.configure(it) }
        }
        return super.createPrimaryScene(view)
    }


}