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

import hep.dataforge.exceptions.StorageException;
import hep.dataforge.io.envelopes.Envelope;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.storage.api.StateLoader;
import hep.dataforge.storage.commons.AbstractNetworkListener;
import hep.dataforge.storage.commons.LoaderFactory;
import hep.dataforge.storage.commons.StoragePlugin;
import hep.dataforge.storage.filestorage.FileStorage;
import hep.dataforge.storage.servlet.SorageRatpackHandler;
import inr.numass.storage.NumassStorage;
import java.io.File;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.handling.Chain;
import ratpack.server.RatpackServer;
import ratpack.server.RatpackServerSpec;
import ratpack.server.ServerConfigBuilder;

/**
 *
 * @author darksnake
 */
public class NumassServer extends AbstractNetworkListener {

    public static final String DEFAULT_RUN_PATH = "default";
    private final Logger logger = LoggerFactory.getLogger("NUMASS-STORAGE");

    private RatpackServer ratpack;
    private FileStorage root;
    private StateLoader rootState;
    private NumassRun run;

    public NumassServer(FileStorage storage, Meta listnerConfig) {
        super(listnerConfig);
        init(storage);
    }

    public NumassServer(String rootPath, Meta rootConfig, Meta listnerConfig) throws StorageException {
        super(listnerConfig);
        logger.info("Initializing file storage in {}", rootPath);
        init(FileStorage.in(new File(rootPath), rootConfig));
    }

    /**
     * Init the root storage and state loader
     *
     * @param storage
     */
    private void init(FileStorage storage) {
        new StoragePlugin().startGlobal();
        this.root = storage;
        try {
            rootState = LoaderFactory.buildStateLoder(storage, "@numass", null);
            updateRun();
        } catch (StorageException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void open() throws Exception {
        super.open();
        int port = meta().getInt("ratpack.port", 8336);
        ratpack = RatpackServer.start((RatpackServerSpec server) -> server
                .serverConfig((ServerConfigBuilder config) -> config.port(port))
                .handlers((Chain chain) -> chain
                        .get(new NumassRootHandler(this))
                        .get("storage", new SorageRatpackHandler(root))
                )
        );
    }

    private void startRun(Meta annotation) throws StorageException {
        String path = annotation.getString("path", DEFAULT_RUN_PATH);
        Meta meta = annotation.getNode("meta", null);
        run = new NumassRun(path, new NumassStorage(root, path, meta), getResponseFactory());
        getRootState().setValue("numass.current.run", path);
    }

    /**
     *
     * @param message
     * @return
     */
    @Override
    public Envelope respond(Envelope message) {
        Meta meta = message.meta();
//        ByteBuffer data = message.getData();

        //switch message type
        String type = meta.getString("type", "numass.state");
        switch (type) {
            case "numass.storage":
                return getRun().getStorage().respond(message);
            case "numass.state":
                return getRootState().respond(message);
            case "numass.data":
            case "numass.run.state":
                return getRun().respond(message);
            case "numass.control":
                return getResponseFactory()
                        .errorResponseBase(message,
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
        this.run = new NumassRun(currentRun, new NumassStorage(root, currentRun, null), getResponseFactory());
    }

    /**
     * Return current run parameters
     *
     * @return
     */
    public Envelope getCurrentRun() {
        MetaBuilder runAn = new MetaBuilder("run")
                .putValue("path", getRun().getRunPath());
        if (!run.meta().isEmpty()) {
            runAn.putNode(getRun().meta());
        }

        return getResponseFactory().responseBase("numass.run.response")
                .putMetaNode(runAn.build()).build();
    }

    /**
     * Reset run to default
     */
    public void resetRun() throws StorageException {
        getRootState().setValue("numass.current.run", DEFAULT_RUN_PATH);
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
            return getResponseFactory().errorResponseBase("numass.run.response", ex).build();
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
//                        .putValue("type", "auth")
//                        .putValue("message", "Authorisation faild. Need the role '"+requiredRole+"'")
//                        .build()
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

//    private class NumassRootHandler implements Handler {
//
//        @Override
//        public void handle(Context c) throws Exception {
//            c.getResponse().contentType("text/html");
//            StringBuilder b = new StringBuilder();
//            renderHTMLHeader(b);
//            b.append("<h1> Server configuration </h1>\n");
//            if (!meta().isEmpty()) {
//                b.append("<h3> Server metadata: </h3>\n");
//                b.append(writer.writeString(meta()));
//                b.append("\n");
//            }
//            if (getRootState() != null) {
//                b.append("<h3> Current root state: </h3>\n");
//                renderStates(b, getRootState());
//            }
//            if (getRun() != null) {
//                b.append("<h1> Current run configuration </h1>\n");
//                if (!run.meta().isEmpty()) {
//                    b.append("<h3> Run metadata: </h3>\n");
//                    b.append(writer.writeString(getRun().meta()));
//                    b.append("\n");
//                }
//                StateLoader runStates = getRun().getStates();
//                if (!runStates.isEmpty()) {
//                    b.append("<h3> Current run state: </h3>\n");
//                    renderStates(b, runStates);
//                }
//
//                b.append("<h2> Current run storage content: </h2>\n");
//                NumassStorage storage = getRun().getStorage();
//                try {
//                    renderStorage(c, b, storage);
//                } catch (StorageException ex) {
//                    b.append("\n<strong>Error reading sotrage structure!!!</strong>\n");
//                }
//            }
//            renderHTMLFooter(b);
//            c.render(b);
//        }
//
//        private void renderStorage(Context ctx, StringBuilder b, Storage storage) throws StorageException {
//            b.append("<div class=\"shifted\">\n");
//            for (Storage shelf : storage.shelves().values()) {
//                b.append(String.format("<p><strong>+ %s</strong></p>%n", shelf.getName()));
//                renderStorage(ctx, b, shelf);
//            }
//            b.append("<div class=\"shifted\">\n");
//            for (Loader loader : storage.loaders().values()) {
//                renderLoader(ctx, b, loader);
//            }
//            b.append("</div>\n");
//            b.append("</div>\n");
//        }
//
//        private void renderLoader(Context ctx, StringBuilder b, Loader loader) {
//            String href = "/storage?path="+loader.getFullPath();
//            b.append(String.format("<p><a href=\"%s\">%s</a> (%s)</p>", href, loader.getName(), loader.getType()));
//        }
//
//    }
}
