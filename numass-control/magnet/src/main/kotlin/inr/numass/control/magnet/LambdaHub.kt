package inr.numass.control.magnet

import hep.dataforge.context.Context
import hep.dataforge.control.devices.AbstractDevice
import hep.dataforge.control.devices.Device
import hep.dataforge.control.devices.DeviceHub
import hep.dataforge.control.ports.Port
import hep.dataforge.control.ports.PortFactory
import hep.dataforge.description.ValueDef
import hep.dataforge.kodex.useEachMeta
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import hep.dataforge.states.StateDef
import hep.dataforge.values.ValueType
import java.util.*
import java.util.stream.Stream
import kotlin.collections.ArrayList

@StateDef(value = ValueDef(name = "address", type = arrayOf(ValueType.NUMBER), info = "Current active magnet"))
class LambdaHub(context: Context, meta: Meta) : DeviceHub, AbstractDevice(context, meta) {

    private val magnets = ArrayList<LambdaMagnet>();

    private val port: Port = buildPort()
    private val controller = LambdaPortController(context, port)

    init {
        meta.useEachMeta("magnet") {
            magnets.add(LambdaMagnet(controller, it))
        }

        meta.useEachMeta("bind") {
            TODO("add binding")
        }
    }

    private fun buildPort(): Port {
        val portMeta = meta.getMetaOrEmpty("port")
        return if(portMeta.getString("type") == "debug"){
            VirtualLambdaPort(portMeta)
        } else{
            PortFactory.build(portMeta)
        }
    }


    override fun init() {
        super.init()
        controller.open()
    }

    override fun shutdown() {
        super.shutdown()
        controller.close()
        port.close()
    }

    override fun optDevice(name: Name): Optional<Device> =
            magnets.stream().filter { it.name == name.toUnescaped() }.map { it as Device }.findFirst()

    override val deviceNames: Stream<Name>
        get() = magnets.stream().map { Name.ofSingle(it.name) }
}