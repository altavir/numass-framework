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
import hep.dataforge.io.envelopes.DefaultEnvelopeWriter;
import hep.dataforge.io.envelopes.Envelope;
import hep.dataforge.io.envelopes.EnvelopeBuilder;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.storage.commons.MessageFactory;
import hep.dataforge.storage.commons.StorageManager;
import hep.dataforge.storage.commons.StorageUtils;
import hep.dataforge.values.Value;
import inr.numass.storage.NumassStorage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipUtil;

/**
 *
 * @author darksnake
 */
public class NumassClient extends MessageFactory implements Closeable {

    Socket socket;

    public NumassClient(String address, int port) throws IOException {
        socket = new Socket(address, port);
        socket.setSoTimeout(300);
    }

    @Override
    public void close() throws IOException {
        if (!socket.isClosed()) {
            write(terminator(), socket.getOutputStream());
        }
        socket.close();
    }

    public Envelope sendAndRecieve(Envelope message) {
        try {
            write(message, socket.getOutputStream());
            return read(socket.getInputStream());
        } catch (IOException ex) {
            LoggerFactory.getLogger(getClass()).error("Error in envelope exchange", ex);
            return errorResponseBase(message, ex).build();
        }
    }

    private Envelope read(InputStream is) throws IOException {
        return new DefaultEnvelopeReader().readWithData(is);
    }

    private void write(Envelope envelope, OutputStream os) throws IOException {
        new DefaultEnvelopeWriter().write(os, envelope);
        os.flush();
    }

    private EnvelopeBuilder requestActionBase(String type, String action) {
        return requestBase(type).putMetaValue("action", action);
    }

    public Meta getCurrentRun() {
        return sendAndRecieve(requestActionBase("numass.run", "get").build()).meta();
    }

    public Meta startRun(String name) {
        return sendAndRecieve(requestActionBase("numass.run", "start")
                .putMetaValue("path", name)
                .build()).meta();
    }

    public Meta resetRun() {
        return sendAndRecieve(requestActionBase("numass.run", "reset")
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

            Envelope bin = requestBase("numass.data")
                    .putMetaValue("action", "push")
                    .putMetaValue("path", path)
                    .putMetaValue("name", zipName)
                    .setData(buffer)
                    .build();

            return sendAndRecieve(bin).meta();
        } catch (IOException ex) {
            return StorageUtils.getErrorMeta(ex);
        }
    }

    /**
     * Get state map for given state names from the root state loader. If
     * stateNames is empty, return all states.
     *
     * @param stateName
     * @return
     */
    public Map<String, Value> getStates(String... stateNames) {
        EnvelopeBuilder env = requestActionBase("numass.state", "get");

        if (stateNames.length > 0) {
            env.putMetaValue("name", Arrays.asList(stateNames));
        }

        Meta response = sendAndRecieve(env.build()).meta();
        if (response.getBoolean("success", true)) {
            Map<String, Value> res = new HashMap<>();
            response.getNodes("state").stream().forEach((stateMeta) -> {
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

        return sendAndRecieve(env.build()).meta();
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
        return sendAndRecieve(env.build()).meta();
    }

    public Meta addNote(String text, Instant time) {
        EnvelopeBuilder env = requestActionBase("numass.notes", "push");
        env.putMetaValue("note.text", text);
        if (time != null) {
            env.putMetaValue("note.time", time);
        }
        return sendAndRecieve(env.build()).meta();
    }

    public Meta getNotes(int limit) {
        EnvelopeBuilder env = requestActionBase("numass.notes", "pull");
        if (limit > 0) {
            env.putMetaValue("limit", limit);
        }
        return sendAndRecieve(env.build()).meta();
    }


}
