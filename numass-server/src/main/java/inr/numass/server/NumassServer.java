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

import hep.dataforge.context.Context;
import hep.dataforge.context.ContextAware;
import hep.dataforge.exceptions.StorageException;
import hep.dataforge.io.envelopes.Envelope;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.storage.api.StateLoader;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.commons.AbstractNetworkListener;
import hep.dataforge.storage.commons.LoaderFactory;
import hep.dataforge.storage.commons.StorageManager;
import hep.dataforge.storage.commons.StorageUtils;
import inr.numass.data.storage.NumassStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.server.RatpackServer;

import java.io.IOException;

import static hep.dataforge.io.messages.MessagesKt.errorResponseBase;
import static hep.dataforge.io.messages.MessagesKt.responseBase;

/**
 * @author darksnake
 */
public class NumassServer extends AbstractNetworkListener implements ContextAware {

    public static final String DEFAULT_RUN_PATH = "default";
    private final Logger logger = LoggerFactory.getLogger("NUMASS-STORAGE");

    private RatpackServer ratpack;
    private NumassStorage root;
    private StateLoader rootState;
    private NumassRun run;

    public NumassServer(NumassStorage storage, Meta listenerConfig) {
        super(listenerConfig);
        init(storage);
    }

//    public NumassServer(String rootPath, Meta rootConfig, Meta listnerConfig) throws StorageException {
//        super(listnerConfig);
//        logger.info("Initializing file storage in {}", rootPath);
//        init(FileStorage.in(new File(rootPath), rootConfig));
//    }

    /**
     * Init the root storage and state loader
     *
     * @param storage
     */
    private void init(NumassStorage storage) {
        new StorageManager().startGlobal();
        this.root = storage;
        try {
            rootState = LoaderFactory.buildStateLoder(storage, "@numass", "");
            updateRun();
        } catch (StorageException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void open() throws Exception {
        super.open();
        int port = getMeta().getInt("ratpack.port", 8336);
//        ratpack = RatpackServer.start((RatpackServerSpec server) -> server
//                .serverConfig((ServerConfigBuilder config) -> config
////                        .baseDir(Paths.get(getClass().getResource("/ratpack/.ratpack").toURI()))
//                        .baseDir(BaseDir.find())
//                        .address(InetAddress.getLocalHost())
//                        .port(port))
//                .handlers((Chain chain) -> chain
//                        .files()
//                        .get(new NumassRootHandler(this))
//                        .get("storage", new NumassStorageHandler(root))
//                )
//        );
    }

    @Override
    public Context getContext() {
        return root.getContext();
    }

    private void startRun(Meta meta) throws StorageException {
        String path = meta.getString("path", DEFAULT_RUN_PATH);
        Storage storage = StorageUtils.INSTANCE.getOrBuildShelf(root, path, meta);
        run = new NumassRun(path, storage);
        getRootState().push("numass.current.run", path);
    }

    /**
     * @param message
     * @return
     */
    @Override
    public Envelope respond(Envelope message) {
        Meta meta = message.getMeta();
//        ByteBuffer data = message.getData();

        //switch message type
        String type = meta.getString("type", "numass.state");
        switch (type) {
            case "numass.storage":
                return getRun().getStorage().respond(message);
            case "numass.state":
                return getRootState().respond(message);
            case "numass.data":
            case "numass.notes":
            case "numass.run.state":
                return getRun().respond(message);
            case "numass.control":
                return errorResponseBase(message,
                                new UnknownNumassActionException("numass.control",
                                        UnknownNumassActionException.Cause.IN_DEVELOPMENT))
                        .build();
            case "numass.run":
                return respondToRunCommand(meta);
            default:
                logger.error("unknown message type");
                return null;
        }
    }

    /**
     * update run state from root storage state loader
     */
    private void updateRun() throws StorageException {
        String currentRun = getRootState().getString("numass.current.run", DEFAULT_RUN_PATH);
        Storage storage = root.optShelf(currentRun).get();
        this.run = new NumassRun(currentRun, storage);
    }

    /**
     * Return current run parameters
     *
     * @return
     */
    public Envelope getCurrentRun() {
        MetaBuilder runAn = new MetaBuilder("run")
                .putValue("path", getRun().getRunPath());
        if (!run.getMeta().isEmpty()) {
            runAn.putNode(getRun().getMeta());
        }

        return responseBase("numass.run.response")
                .putMetaNode(runAn.build()).build();
    }

    /**
     * Reset run to default
     */
    public void resetRun() throws StorageException {
        getRootState().push("numass.current.run", DEFAULT_RUN_PATH);
        updateRun();
    }

    private Envelope respondToRunCommand(Meta meta) {
        try {
            String action = meta.getString("action", "get");
            switch (action) {
                case "start":
                    startRun(meta);
                    return getCurrentRun();
                case "get":
                    return getCurrentRun();
                case "reset":
                    resetRun();
                    return getCurrentRun();
                default:
                    throw new UnknownNumassActionException(action, UnknownNumassActionException.Cause.NOT_SUPPORTED);
            }
        } catch (StorageException ex) {
            return errorResponseBase("numass.run.response", ex).build();
        }
    }

    //    private boolean hasAuthorization(String role, Envelope envelope) {
//        return true;
//    }
//
//    /**
//     * to use when we need authorization
//     * @param requiredRole
//     * @return 
//     */
//    private EnvelopeBuilder authFailedResponse(String requiredRole) {
//        return responseBase("error")
//                .setDataType(MESSAGE_FAIL_CODE)
//                .putMetaNode(new AnnotationBuilder("error")
//                        .setValue("type", "auth")
//                        .setValue("message", "Authorisation faild. Need the role '"+requiredRole+"'")
//                        .builder()
//                );
//    }
    @Override
    public void close() throws IOException, InterruptedException {
        super.close();
        try {
            this.getRootState().close();
            root.close();
        } catch (Exception ex) {
            logger.error("Failed to close root sotrage", ex);
        }
        if (ratpack != null && ratpack.isRunning()) {
            try {
                ratpack.stop();
            } catch (Exception ex) {
                logger.error("Failed to stop ratpack server", ex);
            }
        }
    }

    /**
     * @return the rootState
     */
    public StateLoader getRootState() {
        return rootState;
    }

    /**
     * @return the run
     */
    public NumassRun getRun() {
        return run;
    }
}
