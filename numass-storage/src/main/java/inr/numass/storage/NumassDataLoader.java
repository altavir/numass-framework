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

import hep.dataforge.context.Global;
import hep.dataforge.exceptions.StorageException;
import hep.dataforge.io.ColumnedDataReader;
import hep.dataforge.io.envelopes.Envelope;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.storage.api.ObjectLoader;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.filestorage.FileEnvelope;
import hep.dataforge.storage.loaders.AbstractLoader;
import hep.dataforge.tables.Table;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.VFS;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static inr.numass.storage.RawNMPoint.MAX_EVENTS_PER_POINT;
import static org.apache.commons.vfs2.FileType.FOLDER;

/**
 * The reader for numass main detector data directory or zip format;
 *
 * @author darksnake
 */
public class NumassDataLoader extends AbstractLoader implements ObjectLoader<Envelope>, NumassData {

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

        //FIXME envelopes are lazy do we need to do additional lazy evaluations here?
        Map<String, Supplier<Envelope>> items = new LinkedHashMap<>();

        try (FileObject dir = VFS.getManager().resolveFile(url.toString())) {

            for (FileObject it : dir.getChildren()) {
                items.put(it.getName().getBaseName(), () -> readFile(it));
            }

        } catch (Exception ex) {
            LoggerFactory.getLogger(NumassDataLoader.class)
                    .error("Can't load numass data directory " + directory.getName().getBaseName(), ex);
            return null;
        }

        return new NumassDataLoader(storage, name, annotation, items);
    }

    private static Envelope readFile(FileObject file) {
        //VFS file reading seems to work badly in parallel
        synchronized (Global.instance()) {
            String fileName = file.getName().getBaseName();
            if (fileName.equals(META_FRAGMENT_NAME)
                    || fileName.equals(HV_FRAGMENT_NAME)
                    || fileName.startsWith(POINT_FRAGMENT_NAME)) {
                try {
                    return new FileEnvelope(file.getPublicURIString(), true);
                } catch (IOException | ParseException ex) {
                    LoggerFactory.getLogger(NumassDataLoader.class).error("Can't read file envelope", ex);
                    return null;
                }
            } else {
                return null;
            }
        }
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
     * Read numass point from envelope and apply transformation (e.g. debuncing)
     *
     * @param envelope
     * @param transformation
     * @return
     */
    private NMPoint readPoint(Envelope envelope, Function<RawNMPoint, NMPoint> transformation) {
        return transformation.apply(readRawPoint(envelope));
    }

    /**
     * Read raw point. Requires a low of memory.
     *
     * @param envelope
     * @return
     */
    private synchronized RawNMPoint readRawPoint(Envelope envelope) {
        List<NMEvent> events = new ArrayList<>();

        double timeCoef = envelope.meta().getDouble("time_coeff", 50);
        try (ReadableByteChannel inChannel = envelope.getData().getChannel()) {
            ByteBuffer buffer = ByteBuffer.allocate(7 * 10000); // one event is 7b
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            while (inChannel.read(buffer) > 0) {
                buffer.flip();
                while (buffer.hasRemaining()) {
                    short channel = (short) Short.toUnsignedInt(buffer.getShort());
                    long time = Integer.toUnsignedLong(buffer.getInt());
                    byte status = buffer.get(); // status is ignored
                    NMEvent event = new NMEvent(channel, (double) time * timeCoef * 1e-9);
                    events.add(event);
                }
                buffer.clear(); // do something with the data and clear/compact it.
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }


//        LocalDateTime startTime = envelope.meta().get
        double u = envelope.meta().getDouble("external_meta.HV1_value", 0);
        double pointTime;
        if (envelope.meta().hasValue("external_meta.acquisition_time")) {
            pointTime = envelope.meta().getValue("external_meta.acquisition_time").doubleValue();
        } else {
            pointTime = envelope.meta().getValue("acquisition_time").doubleValue();
        }

        //Check if the point is composite
        boolean segmented = envelope.meta().getBoolean("split", false);

        if (!segmented && events.size() > MAX_EVENTS_PER_POINT) {
            pointTime = events.get(events.size() - 1).getTime() - events.get(0).getTime();
        }

        return new RawNMPoint(u, u,
                events,
                pointTime,
                readTime(envelope.meta()));
    }

    /**
     * Read numass point without transformation
     *
     * @param envelope
     * @return
     */
    public NMPoint readPoint(Envelope envelope) {
        return readPoint(envelope, NMPoint::new);
    }

    private Map<String, Supplier<Envelope>> getItems() {
        return itemsProvider;
    }

    @Override
    public Collection<String> fragmentNames() {
        return getItems().keySet();
    }

    @Override
    public Meta meta() {
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
                return new ColumnedDataReader(hvEnvelope.getData().getStream(), "timestamp", "block", "value").toTable();
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

    @Override
    public Stream<NMPoint> stream() {
        return this.getPoints().map(this::readPoint);
    }

    public List<NMPoint> getNMPoints(Function<RawNMPoint, NMPoint> transformation) {
        return this.getPoints().map(env -> readPoint(env, transformation)).collect(Collectors.toList());
    }

    public List<RawNMPoint> getRawPoints() {
        return this.getPoints().map(this::readRawPoint).collect(Collectors.toList());
    }

    private Stream<Envelope> getPoints() {
        return getItems().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(POINT_FRAGMENT_NAME) && entry.getValue() != null)
                .map(entry -> entry.getValue().get())
                .sorted(Comparator.comparing(t -> t.meta().getInt("external_meta.point_index", -1)));

    }

    public boolean isReversed() {
        return meta().getBoolean("iteration_info.reverse", false);
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
        if (meta.hasValue("start_time")) {
            return meta().getValue("start_time").timeValue();
        } else return null;
        //Temporary substitution for meta tag
//        Envelope hvEnvelope = getHVEnvelope();
//        if (hvEnvelope != null) {
//            try {
//                return Value.of(new Scanner(hvEnvelope.getData().getStream()).next()).timeValue();
//            } catch (IOException ex) {
//                return null;
//            }
//        } else {
//            return null;
//        }
    }

    @Override
    public String getDescription() {
        return meta().getString("description", "").replace("\\n", "\n");
    }

    @Override
    public void open() throws Exception {

    }

    /**
     * Return new NumassData using given transformation for each point
     *
     * @param transform
     * @return
     */
    public NumassData applyRawTransformation(Function<RawNMPoint, NMPoint> transform) {
        return new NumassData() {
            @Override
            public String getDescription() {
                return NumassDataLoader.this.getDescription();
            }

            @Override
            public Meta meta() {
                return NumassDataLoader.this.meta();
            }

            @Override
            public Stream<NMPoint> stream() {
                return NumassDataLoader.this.stream();
            }

            @Override
            public boolean isEmpty() {
                return NumassDataLoader.this.isEmpty();
            }

            @Override
            public Instant startTime() {
                return NumassDataLoader.this.startTime();
            }

            @Override
            public String getName() {
                return NumassDataLoader.this.getName();
            }
        };
    }
}
