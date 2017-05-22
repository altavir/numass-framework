package inr.numass.server;

import hep.dataforge.server.ServerManager;
import hep.dataforge.server.ServerObject;
import hep.dataforge.server.storage.StorageServerObject;
import hep.dataforge.storage.api.Storage;
import ratpack.handling.Handler;

/**
 * Created by darksnake on 22-May-17.
 */
public class NumassStorageServerObject extends StorageServerObject {
    public NumassStorageServerObject(ServerManager manager, Storage storage, String path) {
        super(manager, storage, path);
    }

    public NumassStorageServerObject(ServerObject parent, Storage storage) {
        super(parent, storage);
    }

    @Override
    protected Handler buildHandler(Storage storage) {
        return new NumassStorageHandler(getManager(), storage);
    }

    @Override
    protected StorageServerObject buildChildStorageObject(Storage shelf) {
        return new NumassStorageServerObject(this, shelf);
    }
}
