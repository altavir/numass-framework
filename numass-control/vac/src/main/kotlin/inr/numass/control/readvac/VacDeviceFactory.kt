package inr.numass.control.readvac

import hep.dataforge.context.Context
import hep.dataforge.control.devices.DeviceFactory
import hep.dataforge.control.devices.Sensor
import hep.dataforge.meta.Meta
import java.util.stream.Collectors

/**
 * A factory for vacuum measurements collector
 * Created by darksnake on 16-May-17.
 */
class VacDeviceFactory : DeviceFactory {
    override val type: String = "numass.vac"

    private fun buildSensor(context: Context, sensorConfig: Meta): Sensor {
        return when (sensorConfig.getString("sensorType", "")) {
            "mks" -> MKSVacDevice(context, sensorConfig)
            "CM32" -> CM32Device(context, sensorConfig)
            "meradat" -> MeradatVacDevice(context, sensorConfig)
            "baratron" -> MKSBaratronDevice(context, sensorConfig)
            "ThyroCont" -> ThyroContVacDevice(context,sensorConfig)
//            VIRTUAL_SENSOR_TYPE -> VirtualDevice.randomDoubleSensor(context, sensorConfig)
            else -> throw RuntimeException("Unknown vacuum sensor type")
        }
    }

    override fun build(context: Context, config: Meta): VacCollectorDevice {
        val sensors = config.getMetaList("sensor").stream()
                .map { sensorConfig -> buildSensor(context, sensorConfig) }
                .collect(Collectors.toList<Sensor>())

        return VacCollectorDevice(context, config, sensors)
    }

//    override fun buildView(device: Device): DeviceDisplayFX<VacCollectorDevice> {
//        return VacCollectorDisplay();
//    }
}
