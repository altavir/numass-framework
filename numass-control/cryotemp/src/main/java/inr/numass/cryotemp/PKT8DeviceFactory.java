package inr.numass.cryotemp;

import hep.dataforge.context.Context;
import hep.dataforge.control.connections.Connection;
import hep.dataforge.control.connections.Roles;
import hep.dataforge.control.devices.DeviceFactory;
import hep.dataforge.meta.Meta;

import java.util.Objects;

/**
 * Created by darksnake on 09-May-17.
 */
public class PKT8DeviceFactory implements DeviceFactory<PKT8Device> {
    @Override
    public String getType() {
        return PKT8Device.PKT8_DEVICE_TYPE;
    }

    @Override
    public PKT8Device build(Context context, Meta meta) {
        return new PKT8Device(context, meta);
    }

    @Override
    public Connection<PKT8Device> buildConnection(String role, Context context, Meta meta) {
        if(Objects.equals(role, Roles.VIEW_ROLE)){
            return new PKT8Controller();
        } else {
            return DeviceFactory.super.buildConnection(role, context, meta);
        }
    }
}
