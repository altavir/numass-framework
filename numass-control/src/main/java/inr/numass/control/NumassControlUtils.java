package inr.numass.control;

import hep.dataforge.context.Context;
import hep.dataforge.context.Global;
import hep.dataforge.control.connections.Roles;
import hep.dataforge.control.connections.StorageConnection;
import hep.dataforge.control.devices.Device;
import hep.dataforge.exceptions.StorageException;
import hep.dataforge.io.MetaFileReader;
import hep.dataforge.io.XMLMetaReader;
import hep.dataforge.meta.Meta;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.commons.StorageFactory;
import hep.dataforge.storage.commons.StorageManager;
import inr.numass.client.ClientUtils;
import javafx.application.Application;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Created by darksnake on 08-May-17.
 */
public class NumassControlUtils {
    public static final String DEFAULT_CONFIG_LOCATION = "numass-control.xml";

    /**
     * Create a single or multiple storage connections for a device
     *
     * @param device
     * @param config
     */
    public static void connectStorage(Device device, Meta config) {
        //TODO add on reset listener
        if (config.hasMeta("storage") && device.acceptsRole(Roles.STORAGE_ROLE)) {
            String numassRun = ClientUtils.getRunName(config);
            config.getMetaList("storage").forEach(node -> {
                device.getContext().getLogger().debug("Creating storage for device with meta: {}", node);
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

    public static Meta readResourceMeta(String path) {
        try {
            return new XMLMetaReader().read(NumassControlUtils.class.getResourceAsStream(path));
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static Optional<Meta> getConfig(Application app) {
        String configFileName = app.getParameters().getNamed().get("config");
        if (configFileName == null) {
            configFileName = DEFAULT_CONFIG_LOCATION;
        }
        File configFile = new File(configFileName);

        if (configFile.exists()) {
            try {
                Meta config = MetaFileReader.read(configFile).build();
                return Optional.of(config);
            } catch (IOException | ParseException e) {
                throw new RuntimeException(e);
            }
        } else {
            return Optional.empty();
        }
    }


    public static Optional<Meta> findDeviceMeta(Meta config, Predicate<Meta> criterion) {
        return config.getMetaList("device").stream().filter(criterion).findFirst().map(it -> it);
    }

    public static Context setupContext(Meta meta) {
        Context ctx = Global.getContext("NUMASS-CONTROL");
        ctx.pluginManager().getOrLoad(StorageManager.class);
        return ctx;
    }

}
