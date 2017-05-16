package inr.numass.control;

import hep.dataforge.control.connections.StorageConnection;
import hep.dataforge.control.devices.AbstractDevice;
import hep.dataforge.exceptions.StorageException;
import hep.dataforge.storage.api.PointLoader;
import hep.dataforge.tables.DataPoint;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A helper to store points in multiple loaders
 * Created by darksnake on 16-May-17.
 */
public class StorageHelper implements AutoCloseable {
    private final AbstractDevice device;
    private final Map<StorageConnection, PointLoader> loaderMap = new HashMap<>();
    private final Function<StorageConnection, PointLoader> loaderFactory;

    public StorageHelper(AbstractDevice device, Function<StorageConnection, PointLoader> loaderFactory) {
        this.device = device;
        this.loaderFactory = loaderFactory;
    }

    public void push(DataPoint point) {
        if (!device.hasState("storing") || device.getState("storing").booleanValue()) {
            device.forEachConnection("storage", StorageConnection.class, connection -> {
                PointLoader pl = loaderMap.computeIfAbsent(connection, loaderFactory);
                try {
                    pl.push(point);
                } catch (StorageException ex) {
                    device.getLogger().error("Push to loader failed", ex);
                }
            });
        }
    }


    @Override
    public void close() {
        loaderMap.values().forEach(it -> {
            try {
                it.close();
            } catch (Exception ex) {
                device.getLogger().error("Failed to close Loader", ex);
            }
        });
    }
}
