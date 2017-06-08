package inr.numass.control.readvac

import hep.dataforge.context.Context
import hep.dataforge.control.devices.Device
import hep.dataforge.control.devices.Sensor
import hep.dataforge.control.virtual.VirtualDevice
import hep.dataforge.meta.Meta
import inr.numass.control.DeviceViewConnection
import inr.numass.control.DeviceViewFactory
import java.util.stream.Collectors

/**
 * A factory for vacuum measurements collector
 * Created by darksnake on 16-May-17.
 */
class VacDeviceFactory : DeviceViewFactory<VacCollectorDevice> {
    override fun getType(): String {
        return "numass:vac"
    }

    fun buildSensor(context: Context, sensorConfig: Meta): Sensor<Double> {
        when (sensorConfig.getString("sensorType", "")) {
            "mks" -> return MKSVacDevice(context, sensorConfig)
            "CM32" -> return CM32Device(context, sensorConfig)
            "meradat" -> return MeradatVacDevice(context, sensorConfig)
            "baratron" -> return MKSBaratronDevice(context, sensorConfig)
            VirtualDevice.VIRTUAL_SENSOR_TYPE -> return VirtualDevice.randomDoubleSensor(context, sensorConfig)
            else -> throw RuntimeException("Unknown vacuum sensor type")
        }
    }

    override fun build(context: Context, config: Meta): VacCollectorDevice {
        val sensors = config.getMetaList("sensor").stream()
                .map { sensorConfig ->
                    buildSensor(context, sensorConfig)
                }.collect(Collectors.toList<Sensor<Double>>())

        val collector = VacCollectorDevice(context, config)
        collector.setSensors(sensors)
        return collector
    }

    override fun buildView(device: Device): DeviceViewConnection<VacCollectorDevice> {
        return VacCollectorViewConnection();
    }
}
