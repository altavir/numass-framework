package inr.numass.control.magnet.fx

import hep.dataforge.context.Context
import hep.dataforge.control.devices.Device
import hep.dataforge.control.devices.DeviceFactory
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaUtils
import inr.numass.control.NumassControlApplication
import inr.numass.control.magnet.LambdaHub
import javafx.stage.Stage

class MagnetApp: NumassControlApplication<LambdaHub>() {

    override val deviceFactory: DeviceFactory = object :DeviceFactory{
        override val type: String = "numass.lambda"

        override fun build(context: Context, meta: Meta): Device {
            return LambdaHub(context, meta)
        }
    }

    override fun setupStage(stage: Stage, device: LambdaHub) {
        stage.title = "Numass magnet control"
    }

    override fun getDeviceMeta(config: Meta): Meta {
        return MetaUtils.findNode(config,"device"){it.getString("name") == "numass.magnets"}.orElseThrow{RuntimeException("Magnet configuration not found")}
    }
}