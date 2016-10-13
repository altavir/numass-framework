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

import hep.dataforge.data.binary.Binary;
import hep.dataforge.exceptions.StorageException;
import hep.dataforge.io.envelopes.Envelope;
import hep.dataforge.io.envelopes.EnvelopeBuilder;
import hep.dataforge.io.messages.Responder;
import hep.dataforge.meta.Annotated;
import hep.dataforge.meta.Meta;
import hep.dataforge.storage.api.ObjectLoader;
import hep.dataforge.storage.api.StateLoader;
import hep.dataforge.storage.commons.LoaderFactory;
import hep.dataforge.storage.commons.MessageFactory;
import hep.dataforge.values.Value;
import inr.numass.storage.NumassStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.stream.Stream;

import static inr.numass.server.NumassServerUtils.getNotes;

/**
 * This object governs remote access to numass storage and performs reading and
 * writing to default StateLoader
 *
 * @author darksnake
 */
public class NumassRun implements Annotated, Responder {

    public static final String RUN_STATE = "@run";
    public static final String RUN_NOTES = "@notes";

    private final String runPath;
    /**
     * The Numass storage for this run (it could be not root)
     */
    private final NumassStorage storage;

    /**
     * Default state loader for this run
     */
    private final StateLoader states;

    private final ObjectLoader<NumassNote> noteLoader;
    private final MessageFactory factory;

    private final Logger logger;

    //    /**
//     * A set with inverted order of elements (last note first)
//     */
//    private final Set<NumassNote> notes = new TreeSet<>((NumassNote o1, NumassNote o2) -> -o1.time().compareTo(o2.time()));
    public NumassRun(String path, NumassStorage workStorage, MessageFactory factory) throws StorageException {
        this.storage = workStorage;
        this.states = LoaderFactory.buildStateLoder(storage, RUN_STATE, null);
        this.noteLoader = (ObjectLoader<NumassNote>) LoaderFactory.buildObjectLoder(storage, RUN_NOTES, null);
        this.factory = factory;
        this.runPath = path;
        logger = LoggerFactory.getLogger("CURRENT_RUN");
    }

    public Value getState(String name) {
        return states.getValue(name);
    }

    public void setState(String name, Value value) throws StorageException {
        states.setValue(name, value);
    }

    public void setState(String name, Object value) throws StorageException {
        states.setValue(name, Value.of(value));
    }

    public boolean hasState(String name) {
        return states.hasValue(name);
    }

    @Override
    public Envelope respond(Envelope message) {
        Meta meta = message.meta();
        String type = meta.getString("type", "numass.run.state");
        String action = meta.getString("action");
        switch (type) {
            case "numass.run.state":
                return states.respond(message);
            case "numass.data":
                switch (action) {
                    case "push":
                        return pushNumassPoint(message);
                    default:
                        throw new UnknownNumassActionException(action, UnknownNumassActionException.Cause.NOT_SUPPORTED);
                }
            case "numass.notes":
                switch (action) {
                    case "push":
                        return pushNote(message);
                    case "pull":
                        return pullNotes(message);
                    default:
                        throw new UnknownNumassActionException(action, UnknownNumassActionException.Cause.NOT_SUPPORTED);
                }

            default:
                throw new RuntimeException("Wrong message type");
        }
    }

    public synchronized void addNote(String text, Instant time) throws StorageException {
        NumassNote note = new NumassNote(text, time);
        addNote(note);
    }

    @SuppressWarnings("unchecked")
    public synchronized void addNote(NumassNote note) throws StorageException {
        noteLoader.push(note.ref(), note);
    }

    private synchronized Envelope pushNote(Envelope message) {
        try {
            if (message.meta().hasMeta("note")) {
                for (Meta node : message.meta().getMetaList("note")) {
                    addNote(NumassNote.buildFrom(node));
                }
            } else {
                addNote(NumassNote.buildFrom(message.meta()));
            }
            return factory.okResponseBase(message, false, false).build();
        } catch (Exception ex) {
            logger.error("Failed to push note", ex);
            return factory.errorResponseBase(message, ex).build();
        }
    }

    private Envelope pullNotes(Envelope message) {
        EnvelopeBuilder envelope = factory.okResponseBase(message, true, false);
        int limit = message.meta().getInt("limit", -1);
        //TODO add time window and search conditions here
        Stream<NumassNote> stream = getNotes(noteLoader);
        if (limit > 0) {
            stream = stream.limit(limit);
        }
        stream.forEach((NumassNote note) -> envelope.putMetaNode(note.toMeta()));

        return envelope.build();
    }

    private Envelope pushNumassPoint(Envelope message) {
        try {
            String filePath = message.meta().getString("path", "");
            String fileName = message.meta().getString("name")
                    .replace(NumassStorage.NUMASS_ZIP_EXTENSION, "");// removing .nm.zip if it is present
            storage.pushNumassData(filePath, fileName, Binary.readToBuffer(message.getData()));
            //TODO add checksum here
            return factory.okResponseBase("numass.data.push.response", false, false).build();
        } catch (StorageException | IOException ex) {
            logger.error("Failed to push point", ex);
            return factory.errorResponseBase("numass.data.push.response", ex).build();
        }
    }

    @Override
    public Meta meta() {
        return storage.meta();
    }

    public NumassStorage getStorage() {
        return storage;
    }

    public String getRunPath() {
        return runPath;
    }

    public StateLoader getStates() {
        return states;
    }

}
