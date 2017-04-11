package inr.numass.server;

import hep.dataforge.io.MetaFileReader;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.SimpleConfigurable;
import hep.dataforge.storage.filestorage.FileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Objects;

/**
 * Created by darksnake on 11-Apr-17.
 */
public class ServerRunner extends SimpleConfigurable implements AutoCloseable {
    public static final String SERVER_CONFIG_PATH = "numass-server.xml";
    private static final String NUMASS_REPO_ELEMENT = "numass.repository";
    private static final String LISTENER_ELEMENT = "listener";
    private static final String NUMASS_REPO_PATH_PROPERTY = "numass.repository.path";
    private final Logger logger = LoggerFactory.getLogger("NUMASS-SERVER");
    FileStorage root;
    NumassServer listener;

    public ServerRunner() throws IOException, ParseException {
//        Global.instance().pluginManager().load(StorageManager.class);

        File configFile = new File(SERVER_CONFIG_PATH);
        if (configFile.exists()) {
            logger.info("Trying to read server configuration from {}", SERVER_CONFIG_PATH);
            configure(MetaFileReader.read(configFile));
        }
    }

    public static void main(String[] args) {
        try (ServerRunner r = new ServerRunner()) {
            r.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String in = null;
            while (!Objects.equals(in, "exit")) {
                in = reader.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ServerRunner start() throws Exception {
        String repoPath = meta().getString(NUMASS_REPO_PATH_PROPERTY, ".");
        Meta repoConfig = null;
        if (meta().hasMeta(NUMASS_REPO_ELEMENT)) {
            repoConfig = meta().getMeta(NUMASS_REPO_ELEMENT);
        }
        logger.info("Initializing file storage in {}", repoPath);
        root = FileStorage.in(new File(repoPath), repoConfig);

        logger.info("Starting numass server");
        if (root != null) {
            root.open();
            Meta listenerConfig = null;
            if (meta().hasMeta(LISTENER_ELEMENT)) {
                listenerConfig = meta().getMeta(LISTENER_ELEMENT);
            }

            listener = new NumassServer(root, listenerConfig);
            listener.open();
            logger.info("Successfully started numass server");
        } else {
            logger.error("Root storage not initialized");
        }

        return this;
    }

    @Override
    public void close() {
        logger.info("Stopping numass server");
        if (listener != null) {
            try {
                listener.close();
            } catch (Exception e) {
                logger.error("Failed to close listener", e);
            }
        }


        if (root != null) {
            try {
                root.close();
            } catch (Exception ex) {
                logger.error("Error while closing storage", ex);
            }
        }
    }
}
