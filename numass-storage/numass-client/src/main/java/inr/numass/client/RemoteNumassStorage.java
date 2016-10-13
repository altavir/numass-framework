package inr.numass.client;

import hep.dataforge.exceptions.StorageException;
import hep.dataforge.meta.Meta;
import hep.dataforge.storage.api.Loader;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.commons.AbstractStorage;

import java.io.IOException;

/**
 * Created by darksnake on 09-Oct-16.
 */
public class RemoteNumassStorage extends AbstractStorage {
    private NumassClient client;


    private RemoteNumassStorage(Storage parent, String name, Meta annotation) {
        super(parent, name, annotation);
    }

    public RemoteNumassStorage(String name) {
        super(name);
    }

    private String getIP() {
        return meta().getString("numass.server.ip", "192.168.111.1");
    }

    private int getPort() {
        return meta().getInt("numass.server.port", 8335);
    }

    private NumassClient getClient() throws IOException {
        if (client == null) {
            client = new NumassClient(getIP(), getPort());
        }
        return client;
    }

    @Override
    public Loader buildLoader(Meta loaderConfiguration) throws StorageException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Storage buildShelf(String shelfName, Meta shelfConfiguration) throws StorageException {
        return new RemoteNumassStorage(this, shelfName, shelfConfiguration);
    }
}
