package inr.numass.server;

import hep.dataforge.context.Context;
import hep.dataforge.context.Global;
import hep.dataforge.io.MetaFileReader;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.SimpleConfigurable;
import hep.dataforge.storage.commons.StorageManager;
import inr.numass.data.storage.NumassStorage;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;

/**
 * Created by darksnake on 11-Apr-17.
 */
public class ServerRunner extends SimpleConfigurable implements AutoCloseable {
    public static final String SERVER_CONFIG_PATH = "numass-server.xml";
    private static final String NUMASS_REPO_ELEMENT = "numass.repository";
    private static final String LISTENER_ELEMENT = "listener";
    //    private static final String NUMASS_REPO_PATH_PROPERTY = "numass.repository.path";
    NumassStorage root;
    NumassServer listener;
    Context context = Global.INSTANCE.getContext("NUMASS_SERVER");

    public ServerRunner() throws IOException, ParseException {
//        Global.instance().getPluginManager().load(StorageManager.class);

        Path configFile = context.getFile(SERVER_CONFIG_PATH).getPath();
        if (Files.exists(configFile)) {
            context.getLogger().info("Trying to read server configuration from {}", SERVER_CONFIG_PATH);
            configure(MetaFileReader.Companion.read(configFile));
        }
    }

    public static void main(String[] args) {
        try (ServerRunner r = new ServerRunner()) {
            r.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LoggerFactory.getLogger("NUMASS-SERVER").info("Shutting down");
                r.close();
            }));

            while (true) {

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ServerRunner start() throws Exception {
//        String repoPath = meta().getString(NUMASS_REPO_PATH_PROPERTY, ".");

        Meta storageMeta = getConfig().getMetaOrEmpty(NUMASS_REPO_ELEMENT);
        context.getLogger().info("Initializing file storage with meta: {}", storageMeta);
        root = (NumassStorage) StorageManager.Companion.buildStorage(context, storageMeta);

        context.getLogger().info("Starting numass server");
        if (root != null) {
            root.open();
            Meta listenerConfig = null;
            if (getConfig().hasMeta(LISTENER_ELEMENT)) {
                listenerConfig = getConfig().getMeta(LISTENER_ELEMENT);
            }

            listener = new NumassServer(root, listenerConfig);
            listener.open();
            context.getLogger().info("Successfully started numass server");
        } else {
            context.getLogger().error("Root storage not initialized");
        }

        return this;
    }

    @Override
    public void close() {
        context.getLogger().info("Stopping numass server");
        if (listener != null) {
            try {
                listener.close();
            } catch (Exception e) {
                context.getLogger().error("Failed to close listener", e);
            }
        }

        if (root != null) {
            try {
                root.close();
            } catch (Exception ex) {
                context.getLogger().error("Error while closing storage", ex);
            }
        }
    }
}
