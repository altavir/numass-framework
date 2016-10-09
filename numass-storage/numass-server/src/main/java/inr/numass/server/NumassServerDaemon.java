/* 
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package inr.numass.server;

import hep.dataforge.io.MetaFileReader;
import hep.dataforge.meta.Annotated;
import hep.dataforge.meta.Meta;
import hep.dataforge.storage.commons.StorageManager;
import hep.dataforge.storage.filestorage.FileStorage;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * A daemon wrapper for numass server
 *
 * @author darksnake
 */
public class NumassServerDaemon implements Daemon, Annotated {

    private final Logger logger = LoggerFactory.getLogger("NUMASS-SERVER");

    public static final String SERVER_CONFIG_PATH = "numass-server.xml";
    private static final String NUMASS_REPO_ELEMENT = "numass.repository";
    private static final String LISTENER_ELEMENT = "listener";
    private static final String NUMASS_REPO_PATH_PROPERTY = "numass.repository.path";

    Meta serverConfig;
    FileStorage root;
    NumassServer listener;

    @Override
    public void destroy() {
        if (root != null) {
            try {
                root.close();
            } catch (Exception ex) {
                logger.error("Error while closing storage", ex);
            }
        }
    }

    @Override
    public Meta meta() {
        if (serverConfig != null) {
            return serverConfig;
        } else {
            return Meta.buildEmpty("numass-server");
        }
    }

    @Override
    public void init(DaemonContext dc) throws DaemonInitException, Exception {
        logger.info("Starting numass server daemon");
        logger.info("Starting storage plugin");
        new StorageManager().startGlobal();

        File configFile = new File(SERVER_CONFIG_PATH);
        if (configFile.exists()) {
            logger.info("Trying to read server configuration from {}", SERVER_CONFIG_PATH);
            serverConfig = MetaFileReader.read(configFile);
        }

        String repoPath = meta().getString(NUMASS_REPO_PATH_PROPERTY, "/home/numass-storage/");
        Meta repoConfig = null;
        if (meta().hasMeta(NUMASS_REPO_ELEMENT)) {
            repoConfig = meta().getMeta(NUMASS_REPO_ELEMENT);
        }
        logger.info("Initializing file storage in {}", repoPath);
        root = FileStorage.in(new File(repoPath), repoConfig);
    }

    @Override
    public void start() throws Exception {
        logger.info("Starting numass server daemon");
        if (root != null) {
            root.open();
            Meta listenerConfig = null;
            if (meta().hasMeta(LISTENER_ELEMENT)) {
                listenerConfig = meta().getMeta(LISTENER_ELEMENT);
            }

            listener = new NumassServer(root, listenerConfig);
            listener.open();
            logger.info("Sucessfully started numass server");
        } else {
            logger.error("Root storage not initialized");
        }
    }

    @Override
    public void stop() throws Exception {
        logger.info("Stoping numass server daemon");
        if (listener != null) {
            listener.close();
        }
    }

}
