package inr.numass.control;

import hep.dataforge.control.connections.Roles;
import hep.dataforge.control.connections.StorageConnection;
import hep.dataforge.control.devices.Device;
import hep.dataforge.exceptions.StorageException;
import hep.dataforge.meta.Meta;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.commons.StorageFactory;
import inr.numass.client.ClientUtils;

/**
 * Created by darksnake on 08-May-17.
 */
public class NumassConnections {

    /**
     * Create a single or multiple storage connections for a device
     * @param device
     * @param config
     */
    public static void connectStorage(Device device, Meta config) {
        //TODO add on reset listener
        if (config.hasMeta("storage")) {
            String numassRun = ClientUtils.getRunName(config);
            config.getMetaList("storage").forEach(node -> {
                Storage storage = StorageFactory.buildStorage(device.getContext(), node);
                if (!numassRun.isEmpty()) {
                    try {
                        storage = storage.buildShelf(numassRun, Meta.empty());
                    } catch (StorageException e) {
                        device.getContext().getLogger().error("Failed to build shelf", e);
                    }
                }
                device.connect(new StorageConnection(storage), Roles.STORAGE_ROLE);
            });
        }
    }

}
