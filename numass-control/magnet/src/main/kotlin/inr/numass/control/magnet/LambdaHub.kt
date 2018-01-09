package inr.numass.control.magnet

import hep.dataforge.context.Context
import hep.dataforge.control.devices.AbstractDevice
import hep.dataforge.control.devices.Device
import hep.dataforge.control.devices.DeviceHub
import hep.dataforge.control.devices.StateDef
import hep.dataforge.control.ports.Port
import hep.dataforge.control.ports.PortFactory
import hep.dataforge.description.ValueDef
import hep.dataforge.kodex.useEachMeta
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
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
            magnets.add(LambdaMagnet(context, it, controller))
        }
    }

    private fun buildPort(): Port{
        val portName = meta.getString("port");
        return if(portName.startsWith("virtual")){
            VirtualLambdaPort(meta)
        } else{
            PortFactory.build(portName);
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

    override fun getDeviceNames(): Stream<Name> = magnets.stream().map { Name.ofSingle(it.name) }
}