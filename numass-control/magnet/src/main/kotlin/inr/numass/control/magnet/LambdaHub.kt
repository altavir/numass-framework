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
import inr.numass.control.DeviceDisplayFX
import inr.numass.control.DeviceView
import inr.numass.control.getDisplay
import javafx.scene.Parent
import tornadofx.*
import java.util.*
import java.util.stream.Stream
import kotlin.collections.ArrayList

@StateDef(value = ValueDef(name = "address", type = arrayOf(ValueType.NUMBER), info = "Current active magnet"))
@DeviceView(LambdaHubDisplay::class)
class LambdaHub(context: Context, meta: Meta) : DeviceHub, AbstractDevice(context, meta) {

    val magnets = ArrayList<LambdaMagnet>();

    private val port: Port = buildPort()
    private val controller = LambdaPortController(context, port)

    init {
        meta.useEachMeta("magnet") {
            magnets.add(LambdaMagnet(controller, it))
        }

        meta.useEachMeta("bind") { bindMeta ->
            val first = magnets.find { it.name == bindMeta.getString("first") }
            val second = magnets.find { it.name == bindMeta.getString("second") }
            val delta = bindMeta.getDouble("delta")
            bind(first!!, second!!, delta)
            logger.info("Bound magnet $first to magnet $second with delta $delta")
        }
    }

    /**
     * Add symmetric non-blocking conditions to ensure currents in two magnets have difference within given value.
     * @param controller
     * @param difference
     */
    fun bind(first: LambdaMagnet, second: LambdaMagnet, difference: Double) {
        first.bound = { i -> Math.abs(second.current.doubleValue - i) <= difference }
        second.bound = { i -> Math.abs(first.current.doubleValue - i) <= difference }
    }

    private fun buildPort(): Port {
        val portMeta = meta.getMetaOrEmpty("port")
        return if (portMeta.getString("type") == "debug") {
            VirtualLambdaPort(portMeta)
        } else {
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

class LambdaHubDisplay : DeviceDisplayFX<LambdaHub>() {
    override fun buildView(device: LambdaHub): UIComponent? {
        return object : View() {
            override val root: Parent = vbox {
                device.magnets.forEach {
                    this.add(it.getDisplay().view!!)
                }
            }

        }
    }
}