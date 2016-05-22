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
package inr.numass.storage;

import hep.dataforge.context.GlobalContext;
import hep.dataforge.data.binary.Binary;
import hep.dataforge.exceptions.StorageException;
import hep.dataforge.io.ColumnedDataReader;
import hep.dataforge.io.envelopes.DefaultEnvelopeReader;
import hep.dataforge.io.envelopes.Envelope;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.storage.api.ObjectLoader;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.loaders.AbstractLoader;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.Table;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import static org.apache.commons.vfs2.FileType.FOLDER;
import org.apache.commons.vfs2.VFS;
import org.slf4j.LoggerFactory;

/**
 * The reader for numass main detector data directory or zip format;
 *
 * @author darksnake
 */
public class NumassDataLoader extends AbstractLoader implements ObjectLoader<Envelope>, NumassData {
    //FIXME administer resource release

    /**
     * The name of informational meta file in numass data directory
     */
    public static final String META_FRAGMENT_NAME = "meta";

    /**
     * The beginning of point fragment name
     */
    public static final String POINT_FRAGMENT_NAME = "p";

    /**
     * The beginning of hv fragment name
     */
    public static final String HV_FRAGMENT_NAME = "voltage";

    public static NumassDataLoader fromLocalDir(Storage storage, File directory) throws IOException {
        return fromDir(storage, VFS.getManager().toFileObject(directory), null);
    }

    public static NumassDataLoader fromZip(Storage storage, FileObject zipFile) throws IOException {
        FileObject zipRoot = VFS.getManager().createFileSystem(zipFile);
        return fromDir(storage, zipRoot, zipFile.getName().getBaseName());
    }

    /**
     * Construct numass loader from directory
     *
     * @param storage
     * @param directory
     * @return
     * @throws IOException
     */
    public static NumassDataLoader fromDir(Storage storage, FileObject directory, String name) throws IOException {
        if (directory.getType() != FOLDER || !directory.exists()) {
            throw new IllegalArgumentException("numass data directory reuired");
        }
        Meta annotation = new MetaBuilder("loader")
                .putValue("type", "numass")
                .putValue("numass.loaderFormat", "dir")
                .putValue("file.timeCreated", Instant.ofEpochMilli(directory.getContent().getLastModifiedTime()))
                .build();

        if (name == null || name.isEmpty()) {
            name = directory.getName().getBaseName();
        }

        URL url = directory.getURL();

        Map<String, Supplier<Envelope>> items = new LinkedHashMap<>();

        FileObject dir = null;
        try {
            dir = VFS.getManager().resolveFile(url.toString());

            for (FileObject it : dir.getChildren()) {
                items.put(it.getName().getBaseName(), () -> readFile(it));
            }

        } catch (Exception ex) {
            LoggerFactory.getLogger(NumassDataLoader.class)
                    .error("Can't load numass data directory " + directory.getName().getBaseName(), ex);
            return null;
        } finally {
            if (dir != null) {
                try {
                    dir.close();
                } catch (FileSystemException ex) {
                    LoggerFactory.getLogger(NumassDataLoader.class)
                            .error("Can't close remote directory", ex);
                }
            }
        }

        return new NumassDataLoader(storage, name, annotation, items);
    }

    private static Envelope readFile(FileObject file) {
        //VFS file reading seems to work basly in parallel
        synchronized (GlobalContext.instance()) {
            String fileName = file.getName().getBaseName();
            if (fileName.equals(META_FRAGMENT_NAME)
                    || fileName.equals(HV_FRAGMENT_NAME)
                    || fileName.startsWith(POINT_FRAGMENT_NAME)) {
                try {
                    return readStream(file.getContent().getInputStream());
                } catch (FileSystemException ex) {
                    LoggerFactory.getLogger(NumassDataLoader.class).error("Can't read file envelope", ex);
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    /**
     * Read numass point from envelope and apply transformation (e.g. debuncing)
     *
     * @param envelope
     * @param transformation
     * @return
     */
    public NMPoint readPoint(Envelope envelope, Function<RawNMPoint, NMPoint> transformation) {
        List<NMEvent> events = new ArrayList<>();
        ByteBuffer buffer;
        try {
            buffer = Binary.readToBuffer(envelope.getData());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        buffer.position(0);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        double timeCoef = envelope.meta().getDouble("time_coeff", 50);
        while (buffer.hasRemaining()) {
            try {
                short channel = (short) Short.toUnsignedInt(buffer.getShort());
                long time = Integer.toUnsignedLong(buffer.getInt());
                byte status = buffer.get();
                NMEvent event = new NMEvent(channel, (double) time * timeCoef * 1e-9);
                events.add(event);
            } catch (Exception ex) {
                //LoggerFactory.getLogger(MainDataReader.class).error("Error in data format", ex);
                throw new RuntimeException(ex);
            }
        }

//        LocalDateTime startTime = envelope.meta().get
        double u = envelope.meta().getDouble("external_meta.HV1_value", 0);
        double pointTime;
        if (envelope.meta().hasValue("external_meta.acquisition_time")) {
            pointTime = envelope.meta().getValue("external_meta.acquisition_time").doubleValue();
        } else {
            pointTime = envelope.meta().getValue("acquisition_time").doubleValue();
        }
        RawNMPoint raw = new RawNMPoint(u, u,
                events,
                pointTime,
                readTime(envelope.meta()));

        return transformation.apply(raw);
    }

    /**
     * "start_time": "2016-04-20T04:08:50",
     *
     * @param meta
     * @return
     */
    private static Instant readTime(Meta meta) {
        if (meta.hasValue("start_time")) {
            return meta.getValue("start_time").timeValue();
        } else {
            return Instant.EPOCH;
        }
    }

    /**
     * Read numass point without transformation
     *
     * @param envelope
     * @return
     */
    public NMPoint readPoint(Envelope envelope) {
        return readPoint(envelope, (p) -> new NMPoint(p));
    }

    private static Envelope readStream(InputStream stream) {
//        Tag override = new Tag((short) 1, (short) 1, -1, 256, -1);
        try {
            return new DefaultEnvelopeReader().read(stream);
//            return new DefaultEnvelopeReader().customRead(stream, override.asProperties());
        } catch (IOException ex) {
            LoggerFactory.getLogger(NumassDataLoader.class).warn("Can't read a fragment from numass zip or directory", ex);
            return null;
        }
    }

    private final Map<String, Supplier<Envelope>> itemsProvider;

    private NumassDataLoader(Storage storage, String name, Meta annotation) {
        super(storage, name, annotation);
        itemsProvider = new HashMap<>();
        readOnly = true;
    }

    private NumassDataLoader(Storage storage, String name, Meta annotation, Map<String, Supplier<Envelope>> items) {
        super(storage, name, annotation);
        this.itemsProvider = items;
        readOnly = true;
    }

    private Map<String, Supplier<Envelope>> getItems() {
        return itemsProvider;
    }

    @Override
    public Collection<String> fragmentNames() {
        return getItems().keySet();
    }

    @Override
    public Meta getInfo() {
        return getItems()
                .get(META_FRAGMENT_NAME)
                .get()
                .meta();
    }

    @Override
    public Supplier<Table> getHVData() {
        Envelope hvEnvelope = getHVEnvelope();
        if (hvEnvelope == null) {
            return () -> null;
        }
        return () -> {
            try {
                return new ColumnedDataReader(hvEnvelope.getData().getStream(), "timestamp", "block", "value").toDataSet();
            } catch (IOException ex) {
                LoggerFactory.getLogger(getClass()).error("Failed to load HV data from file", ex);
                return null;
            }
        };
    }

    private Envelope getHVEnvelope() {
        if (getItems().containsKey(HV_FRAGMENT_NAME)) {
            return getItems().get(HV_FRAGMENT_NAME).get();
        } else {
            return null;
        }
    }

//    public Envelope getHvData() {
//        return hvData;
//    }
    @Override
    public List<NMPoint> getNMPoints() {
        return this.getPoints().stream().parallel().map(env -> readPoint(env)).collect(Collectors.toList());
    }

    private List<Envelope> getPoints() {
        return getItems().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(POINT_FRAGMENT_NAME) && entry.getValue() != null)
                .map(entry -> entry.getValue().get())//TODO check for nulls?
                .sorted((Envelope t, Envelope t1)
                        -> t.meta().getInt("external_meta.point_index", -1).compareTo(t1.meta().getInt("external_meta.point_index", -1)))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isEmpty() {
        return getItems().isEmpty();
    }

    @Override
    public Envelope pull(String header) {
        //PENDING read data to memory?
        return getItems().get(header).get();
    }

    @Override
    public void push(String header, Envelope data) throws StorageException {
        tryPush();
    }

    @Override
    public Envelope respond(Envelope message) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Instant startTime() {
        if (meta().hasValue("file.timeCreated")) {
            return meta().getValue("file.timeCreated").timeValue();
        } else {
            return null;
        }

    }

    @Override
    public String getDescription() {
        return meta().getString("description", "").replace("\\n", "\n");
    }

    @Override
    public void open() throws Exception {

    }
}
