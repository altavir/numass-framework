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
package inr.numass.client;

import hep.dataforge.io.envelopes.DefaultEnvelopeReader;
import hep.dataforge.io.envelopes.DefaultEnvelopeType;
import hep.dataforge.io.envelopes.Envelope;
import hep.dataforge.io.envelopes.EnvelopeBuilder;
import hep.dataforge.io.messages.Responder;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.storage.commons.MessageFactory;
import hep.dataforge.storage.commons.StorageUtils;
import hep.dataforge.values.Value;
import hep.dataforge.values.Values;
import inr.numass.data.storage.NumassStorage;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipUtil;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author darksnake
 */
public class NumassClient implements AutoCloseable, Responder {

    Socket socket;
    MessageFactory mf = new MessageFactory();

    public NumassClient(String address, int port) throws IOException {
        socket = new Socket(address, port);
        socket.setSoTimeout(300);
    }


    public NumassClient(Meta meta) throws IOException {
        this(meta.getString("ip", "192.168.111.1"), meta.getInt("port", 8335));
    }

    @Override
    public void close() throws IOException {
        if (!socket.isClosed()) {
            write(mf.terminator(), socket.getOutputStream());
        }
        socket.close();
    }

    @Override
    public Envelope respond(Envelope message) {
        try {
            write(message, socket.getOutputStream());
            return read(socket.getInputStream());
        } catch (IOException ex) {
            LoggerFactory.getLogger(getClass()).error("Error in envelope exchange", ex);
            return mf.errorResponseBase(message, ex).build();
        }
    }

    private Envelope read(InputStream is) throws IOException {
        return new DefaultEnvelopeReader().readWithData(is);
    }

    private void write(Envelope envelope, OutputStream os) throws IOException {
        DefaultEnvelopeType.instance.getWriter().write(os, envelope);
        os.flush();
    }

    private EnvelopeBuilder requestActionBase(String type, String action) {
        return mf.requestBase(type).putMetaValue("action", action);
    }

    public Meta getCurrentRun() {
        return respond(requestActionBase("numass.run", "get").build()).meta();
    }

    public Meta startRun(String name) {
        return respond(requestActionBase("numass.run", "start")
                .putMetaValue("path", name)
                .build()).meta();
    }

    public Meta resetRun() {
        return respond(requestActionBase("numass.run", "reset")
                .build()).meta();
    }

    public Meta sendNumassData(String path, String fileName) {
        try {
            File file = new File(fileName);
            ByteBuffer buffer;
            String zipName = null;
            if (file.isDirectory()) {
                File tmpFile = File.createTempFile(file.getName(), NumassStorage.NUMASS_ZIP_EXTENSION);
                tmpFile.deleteOnExit();
                ZipUtil.pack(file, tmpFile);
                zipName = file.getName();
                file = tmpFile;
            }

            if (file.toString().endsWith(NumassStorage.NUMASS_ZIP_EXTENSION)) {
                FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
                buffer = ByteBuffer.allocate((int) channel.size());
                channel.read(buffer);
                if (zipName == null) {
                    zipName = file.getName().replace(NumassStorage.NUMASS_ZIP_EXTENSION, "");
                }
            } else {
                return StorageUtils.getErrorMeta(new FileNotFoundException(fileName));
            }

            Envelope bin = mf.requestBase("numass.data")
                    .putMetaValue("action", "push")
                    .putMetaValue("path", path)
                    .putMetaValue("name", zipName)
                    .setData(buffer)
                    .build();

            return respond(bin).meta();
        } catch (IOException ex) {
            return StorageUtils.getErrorMeta(ex);
        }
    }

    /**
     * Get state map for given state names from the root state loader. If
     * stateNames is empty, return all states.
     *
     * @param stateNames
     * @return
     */
    public Map<String, Value> getStates(String... stateNames) {
        EnvelopeBuilder env = requestActionBase("numass.state", "get");

        if (stateNames.length > 0) {
            env.putMetaValue("name", Arrays.asList(stateNames));
        }

        Meta response = respond(env.build()).meta();
        if (response.getBoolean("success", true)) {
            Map<String, Value> res = new HashMap<>();
            response.getMetaList("state").stream().forEach((stateMeta) -> {
                res.put(stateMeta.getString("name"), stateMeta.getValue("value"));
            });
            return res;
        } else {
            return null;
        }
    }

    /**
     * Set a single state and return resulting envelope meta
     *
     * @param name
     * @param value
     * @return
     */
    public Meta setState(String name, Object value) {
        EnvelopeBuilder env = requestActionBase("numass.state", "set");
        env.putMetaNode(new MetaBuilder("state")
                .setValue("name", name)
                .setValue("value", value)
                .build());

        return respond(env.build()).meta();
    }

    /**
     * Set states and return resulting meta
     *
     * @param stateMap
     * @return
     */
    public Meta setState(Map<String, Value> stateMap) {
        EnvelopeBuilder env = requestActionBase("numass.state", "set");
        stateMap.entrySet().stream().forEach((state) -> {
            env.putMetaNode(new MetaBuilder("state")
                    .setValue("name", state.getKey())
                    .setValue("value", state.getValue())
                    .build());
        });
        return respond(env.build()).meta();
    }

    public Meta addNote(String text, Instant time) {
        EnvelopeBuilder env = requestActionBase("numass.notes", "push");
        env.putMetaValue("note.text", text);
        if (time != null) {
            env.putMetaValue("note.time", time);
        }
        return respond(env.build()).meta();
    }

    public Meta getNotes(int limit) {
        EnvelopeBuilder env = requestActionBase("numass.notes", "pull");
        if (limit > 0) {
            env.putMetaValue("limit", limit);
        }
        return respond(env.build()).meta();
    }

    /**
     * Create remote storage with given meta
     *
     * @param path full path relative to root storage
     * @param meta
     * @return
     */
    public Envelope createStorage(String path, Meta meta) {
        throw new UnsupportedOperationException();
    }

    /**
     * Create remote loader
     *
     * @param shelf full path to the shelf
     * @param name  the name of the loader
     * @param meta  loader meta
     * @return
     */
    public Envelope createLoader(String shelf, String name, Meta meta) {
        throw new UnsupportedOperationException();
    }

    /**
     * Send points to existing point loader
     *
     * @param shelf
     * @param loaderName
     * @param points
     * @return
     */
    public Envelope sendDataPoints(String shelf, String loaderName, Collection<Values> points) {
        throw new UnsupportedOperationException();
    }


}
