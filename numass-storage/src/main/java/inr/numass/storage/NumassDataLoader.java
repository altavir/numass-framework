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

import hep.dataforge.data.binary.Binary;
import hep.dataforge.exceptions.StorageException;
import hep.dataforge.io.envelopes.DefaultEnvelopeReader;
import hep.dataforge.io.envelopes.Envelope;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.storage.api.BinaryLoader;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.loaders.AbstractLoader;
import inr.numass.data.NMEvent;
import inr.numass.data.NMPoint;
import inr.numass.data.NumassData;
import inr.numass.data.RawNMPoint;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import static org.apache.commons.vfs2.FileType.FOLDER;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider;
import org.slf4j.LoggerFactory;

/**
 * The reader for numass main detector data directory or zip format;
 *
 * @author darksnake
 */
public class NumassDataLoader extends AbstractLoader implements BinaryLoader<Envelope>, NumassData {
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
        return fromDir(storage, new DefaultLocalFileProvider().findLocalFile(directory), null);
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
                .build();

        if (name == null || name.isEmpty()) {
            name = directory.getName().getBaseName();
        }

        URL url = directory.getURL();

        return new NumassDataLoader(storage, name, annotation, () -> {
            FileObject dir = null;
            try {
                dir = VFS.getManager().resolveFile(url.toString());

                Map<String, Envelope> items = new HashMap<>();
                for (FileObject it : dir.getChildren()) {
                    Envelope envelope = readFile(it);
                    if (envelope != null) {
                        items.put(it.getName().getBaseName(), envelope);
                    }
                }
                return items;
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
        });
    }

    private static Envelope readFile(FileObject file) throws FileSystemException {
        String fileName = file.getName().getBaseName();
        if (fileName.equals(META_FRAGMENT_NAME)
                || fileName.equals(HV_FRAGMENT_NAME)
                || fileName.startsWith(POINT_FRAGMENT_NAME)) {
            return readStream(file.getContent().getInputStream());
        } else {
            return null;
        }
    }

    /**
     * Read numass point from envelope and apply transformation (e.g. debuncing)
     *
     * @param envelope
     * @param transformation
     * @return
     */
    public static NMPoint readPoint(Envelope envelope, Function<RawNMPoint, NMPoint> transformation) {
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
        RawNMPoint raw = new RawNMPoint(u, u,
                events,
                envelope.meta().getValue("external_meta.acquisition_time").doubleValue(),
                readTime(envelope.meta()));

        return transformation.apply(raw);
    }

    private static Instant readTime(Meta meta) {
        if (meta.hasValue("date") && meta.hasValue("start_time")) {
            LocalDate date = LocalDate.parse(meta.getString("date"), DateTimeFormatter.ofPattern("uuuu.MM.dd"));
            LocalTime time = LocalTime.parse(meta.getString("start_time"));
            LocalDateTime dateTime = LocalDateTime.of(date, time);
            return dateTime.toInstant(ZoneOffset.UTC);
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
    public static NMPoint readPoint(Envelope envelope) {
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

    private final Supplier<Map<String, Envelope>> items;

    private NumassDataLoader(Storage storage, String name, Meta annotation) {
        super(storage, name, annotation);
        items = () -> new HashMap<>();
        readOnly = true;
    }

    private NumassDataLoader(Storage storage, String name, Meta annotation, Supplier<Map<String, Envelope>> items) {
        super(storage, name, annotation);
        this.items = items;
        readOnly = true;
    }

    private Map<String, Envelope> getItems() {
        Map<String, Envelope> map = items.get();
        if (map == null) {
            return Collections.emptyMap();
        } else {
            return map;
        }
    }

    @Override
    public Collection<String> fragmentNames() {
        return getItems().keySet();
    }

    @Override
    public Meta getInfo() {
        return getItems()
                .get(META_FRAGMENT_NAME)
                .meta();
    }

//    public Envelope getHvData() {
//        return hvData;
//    }
    @Override
    public List<NMPoint> getNMPoints() {
        List<NMPoint> res = new ArrayList<>();
        this.getPoints().stream().forEachOrdered((point) -> {
            res.add(readPoint(point));
        });
//        res.sort((NMPoint o1, NMPoint o2) -> o1.getStartTime().compareTo(o2.getStartTime()));
        return res;
    }

    protected List<Envelope> getPoints() {
        List<Envelope> res = new ArrayList<>();
        getItems().forEach((k, v) -> {
            if (k.startsWith(POINT_FRAGMENT_NAME)) {
                if (v != null) {
                    res.add(v);
                }
            }
        });

        res.sort((Envelope t, Envelope t1) -> t.meta().getInt("external_meta.point_index", -1)
                .compareTo(t1.meta().getInt("external_meta.point_index", -1)));

        return res;
    }

    @Override
    public boolean isEmpty() {
        return getItems().isEmpty();
    }

    @Override
    public Envelope pull(String header) {
        //PENDING read data to memory?
        return getItems().get(header);
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
        return null;
//        List<NMPoint> points = getNMPoints();
//        if(!points.isEmpty()){
//            return points.get(0).getStartTime();
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
}
