package inr.numass.control

import hep.dataforge.control.devices.Device
import hep.dataforge.fx.FXObject
import hep.dataforge.fx.fragments.FXFragment
import hep.dataforge.fx.fragments.FragmentWindow
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.Node
import javafx.scene.control.ToggleButton
import tornadofx.*

/**
 * A simple device indicator board
 * Created by darksnake on 11-May-17.
 */
class DeviceInfoView(val device: Device, node: Node? = null) : Fragment(device.name) {

    constructor(pair: Pair<Device, Node?>) : this(pair.first, pair.second);

    val deviceNode = SimpleObjectProperty<Node>();

    var viewButton: ToggleButton by singleAssign();

    override val root = hbox {
        label(device.name)
        add(Indicator.build(device, Device.INITIALIZED_STATE).fxNode)
        viewButton = togglebutton("View") {
            disableProperty().bind(deviceNode.isNull);
        }
    }

    init {
        FragmentWindow(FXFragment.buildFromNode(device.name) { deviceNode.get() })

        if (node != null) {
            deviceNode.set(node);
        } else if (device is FXObject) {
            deviceNode.set(device.fxNode)
        }
    }

    fun setDeviceView(node: Node) {
        deviceNode.set(node);
    }
}


