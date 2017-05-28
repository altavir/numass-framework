package inr.numass.control.readvac;

import hep.dataforge.context.Context;
import hep.dataforge.control.devices.Device;
import hep.dataforge.control.measurements.Sensor;
import hep.dataforge.meta.Meta;
import inr.numass.control.DeviceViewConnection;
import inr.numass.control.DeviceViewFactory;
import inr.numass.control.readvac.fx.VacCollectorView;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A factory for vacuum measurements collector
 * Created by darksnake on 16-May-17.
 */
public class VacDeviceFactory implements DeviceViewFactory {
    @Override
    public String getType() {
        return "numass:vac";
    }

    public Sensor<Double> buildSensor(Context context, Meta sensorConfig) {
        switch (sensorConfig.getString("sensorType", "")) {
            case "mks":
                return new MKSVacDevice(context, sensorConfig);
            case "CM32":
                return new CM32Device(context, sensorConfig);
            case "meradat":
                return new MeradatVacDevice(context, sensorConfig);
            case "baratron":
                return new MKSBaratronDevice(context, sensorConfig);
            default:
                throw new RuntimeException("Unknown vacuum sensor type");
        }
    }

    @Override
    public VacCollectorDevice build(Context context, Meta config) {
        List<Sensor<Double>> sensors = config.getMetaList("sensor").stream()
                .map(sensorConfig -> buildSensor(context, sensorConfig)).collect(Collectors.toList());

        VacCollectorDevice collector = new VacCollectorDevice(context, config);
        collector.setSensors(sensors);
        return collector;
    }

    @Override
    public DeviceViewConnection buildView(Device device) {
        return VacCollectorView.build(device.getContext());
    }
}
