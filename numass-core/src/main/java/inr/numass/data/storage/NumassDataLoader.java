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
package inr.numass.data.storage;

import hep.dataforge.exceptions.StorageException;
import hep.dataforge.io.ColumnedDataReader;
import hep.dataforge.io.envelopes.Envelope;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.providers.Provider;
import hep.dataforge.storage.api.ObjectLoader;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.filestorage.FileStorage;
import hep.dataforge.storage.loaders.AbstractLoader;
import hep.dataforge.tables.Table;
import hep.dataforge.values.Value;
import inr.numass.data.api.NumassPoint;
import inr.numass.data.api.NumassSet;
import inr.numass.data.legacy.NumassFileEnvelope;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;


/**
 * The reader for numass main detector data directory or zip format;
 *
 * @author darksnake
 */
public class NumassDataLoader extends AbstractLoader implements ObjectLoader<Envelope>, NumassSet, Provider {


    public static NumassDataLoader fromFile(Storage storage, Path zipFile) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }


    /**
     * Construct numass loader from directory
     *
     * @param storage
     * @param directory
     * @return
     * @throws IOException
     */
    public static NumassDataLoader fromDir(Storage storage, Path directory, String name) throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Numass data directory required");
        }
        Meta annotation = new MetaBuilder("loader")
                .putValue("type", "numass")
                .putValue("numass.loaderFormat", "dir")
//                .putValue("file.timeCreated", Instant.ofEpochMilli(directory.getContent().getLastModifiedTime()))
                .build();

        if (name == null || name.isEmpty()) {
            name = FileStorage.entryName(directory);
        }

        //FIXME envelopes are lazy do we need to do additional lazy evaluations here?
        Map<String, Supplier<Envelope>> items = new LinkedHashMap<>();

        Files.list(directory).filter(file -> {
            String fileName = file.getFileName().toString();
            return fileName.equals(META_FRAGMENT_NAME)
                    || fileName.equals(HV_FRAGMENT_NAME)
                    || fileName.startsWith(POINT_FRAGMENT_NAME);
        }).forEach(file -> {
            try {
                items.put(FileStorage.entryName(file), () -> NumassFileEnvelope.open(file, true));
            } catch (Exception ex) {
                LoggerFactory.getLogger(NumassDataLoader.class)
                        .error("Can't load numass data directory " + FileStorage.entryName(directory), ex);
            }
        });

        return new NumassDataLoader(storage, name, annotation, items);
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

    private NumassDataLoader(Storage storage, String name, Meta meta, Map<String, Supplier<Envelope>> items) {
        super(storage, name, meta);
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
    public Meta meta() {
        return getItems()
                .get(META_FRAGMENT_NAME)
                .get()
                .meta();

    }

    @Override
    public Optional<Table> getHvData() {
        return getHVEnvelope().map(hvEnvelope -> {
                    try {
                        return new ColumnedDataReader(hvEnvelope.getData().getStream(), "timestamp", "block", "value").toTable();
                    } catch (IOException ex) {
                        LoggerFactory.getLogger(getClass()).error("Failed to load HV data from file", ex);
                        return null;
                    }
                }
        );

    }

    private Optional<Envelope> getHVEnvelope() {
        return Optional.ofNullable(getItems().get(HV_FRAGMENT_NAME)).map(Supplier::get);
    }

    private Stream<Envelope> getPointEnvelopes() {
        return getItems().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(POINT_FRAGMENT_NAME) && entry.getValue() != null)
                .map(entry -> entry.getValue().get())
                .sorted(Comparator.comparing(t -> t.meta().getInt("external_meta.point_index", -1)));

    }

    @Override
    public Stream<NumassPoint> getPoints() {
        return getPointEnvelopes().map(ClassicNumassPoint::new);
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
    public Instant getStartTime() {
        return meta.optValue("start_time").map(Value::timeValue).orElseGet(() -> NumassSet.super.getStartTime());
    }

    @Override
    public String getDescription() {
        return meta().getString("description", "").replace("\\n", "\n");
    }

    @Override
    public void open() throws Exception {

    }

}
