package hep.dataforge.control;

import hep.dataforge.control.devices.Device;
import hep.dataforge.io.envelopes.Envelope;
import hep.dataforge.meta.Meta;

/**
 * Created by darksnake on 11-Oct-16.
 */
public class ControlUtils {
    public static String getDeviceType(Meta meta){
        return meta.getString("type");
    }

    public static String getDeviceName(Meta meta){
        return meta.getString("name","");
    }

    public static Envelope getDefaultDeviceResponse(Device device, Envelope request){
        throw new UnsupportedOperationException("Not implemented");
    }
}
