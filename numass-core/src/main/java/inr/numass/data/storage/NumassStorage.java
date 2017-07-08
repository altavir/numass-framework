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

import hep.dataforge.context.Context;
import hep.dataforge.events.Event;
import hep.dataforge.events.EventBuilder;
import hep.dataforge.exceptions.StorageException;
import hep.dataforge.meta.Meta;
import hep.dataforge.storage.filestorage.FileStorage;
import inr.numass.data.api.NumassSet;
import inr.numass.data.legacy.NumassDatFile;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;


/**
 * The file storage containing numass data directories or zips.
 * <p>
 * Any subdirectory is treated as numass data directory. Any zip must have
 * {@code NUMASS_ZIP_EXTENSION} extension to be recognized. Any other files are
 * ignored.
 * </p>
 *
 * @author Alexander Nozik
 */
public class NumassStorage extends FileStorage {

    public static final String NUMASS_ZIP_EXTENSION = ".nm.zip";
    public static final String NUMASS_DATA_LOADER_TYPE = "numassData";

    protected NumassStorage(FileStorage parent, String path, Meta config) throws StorageException {
        super(parent, path, config);
        super.refresh();
    }

    public NumassStorage(Context context, Meta config) throws StorageException {
        super(context, config);
        super.refresh();
    }

    @Override
    protected void updateDirectoryLoaders() {
        try {
            this.loaders.clear();
            Files.list(getDataDir()).forEach(file -> {
                try {
                    if (Files.isDirectory(file)) {
                        Path metaFile = file.resolve(NumassDataLoader.META_FRAGMENT_NAME);
                        if (Files.exists(metaFile)) {
                            this.loaders.put(entryName(file),
                                    NumassDataLoader.fromDir(this, file, null));
                        } else {
                            this.shelves.put(entryName(file),
                                    new NumassStorage(this, entryName(file), meta()));
                        }
                    } else if (file.getFileName().endsWith(NUMASS_ZIP_EXTENSION)) {
                        this.loaders.put(entryName(file), NumassDataLoader.fromFile(this, file));
//                    } else if (file.getFileName().endsWith(".points")) {
//                        try {
//                            loaders.put(getFileName(file),
//                                    FilePointLoader.fromFile(this, file, true));
//                        } catch (Exception ex) {
//                            getLogger().error("Failed to build numass point loader from file {}", file.getName());
//                        }
                    } else {
                        //updating non-numass loader files
                        updateFile(file);
                    }
                } catch (IOException ex) {
                    LoggerFactory.getLogger(getClass()).error("Error while creating numass loader", ex);
                } catch (StorageException ex) {
                    LoggerFactory.getLogger(getClass()).error("Error while creating numass group", ex);
                }
            });
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void pushNumassData(String path, String fileName, ByteBuffer data) throws StorageException {
        if (path == null || path.isEmpty()) {
            pushNumassData(fileName, data);
        } else {
            NumassStorage st = (NumassStorage) buildShelf(path);
            st.pushNumassData(fileName, data);
        }
    }

    /**
     * Read nm.zip content and write it as a new nm.zip file
     *
     * @param fileName
     */
    @SuppressWarnings("unchecked")
    public void pushNumassData(String fileName, ByteBuffer data) throws StorageException {
        //FIXME move zip to internal
        try {
            Path nmFile = getDataDir().resolve(fileName + NUMASS_ZIP_EXTENSION);
            if (Files.exists(nmFile)) {
                LoggerFactory.getLogger(getClass()).warn("Trying to rewrite existing numass data file {}", nmFile.toString());
            }
            try (ByteChannel channel = Files.newByteChannel(nmFile, CREATE, WRITE)) {
                channel.write(data);
            }

            dispatchEvent(NumassDataPointEvent.build(getName(), fileName, (int) Files.size(nmFile)));
        } catch (IOException ex) {
            throw new StorageException(ex);
        }
    }

    @Override
    public NumassStorage createShelf(String path, Meta meta) throws StorageException {
        return new NumassStorage(this, path, meta);
    }

    /**
     * A list of legacy DAT files in the directory
     *
     * @return
     */
    public List<NumassSet> legacyFiles() {
        try {
            List<NumassSet> files = new ArrayList<>();
            Files.list(getDataDir()).forEach(file -> {
                if (Files.isRegularFile(file) && file.getFileName().toString().toLowerCase().endsWith(".dat")) {
                    String name = file.getFileName().toString();
                    try {
                        files.add(new NumassDatFile(file, Meta.empty()));
                    } catch (Exception ex) {
                        LoggerFactory.getLogger(getClass()).error("Error while reading legacy numass file " + file.getFileName(), ex);
                    }
                }
            });
            return files;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public String getDescription() {
        return meta().getString("description", "");
    }

    public static class NumassDataPointEvent extends Event {

        public static final String FILE_NAME_KEY = "fileName";
        public static final String FILE_SIZE_KEY = "fileSize";

        public NumassDataPointEvent(Meta meta) {
            super(meta);
        }

        public static NumassDataPointEvent build(String source, String fileName, int fileSize) {
            return new NumassDataPointEvent(builder(source, fileName, fileSize).buildEventMeta());
        }

        public static EventBuilder builder(String source, String fileName, int fileSize) {
            return EventBuilder.make("numass.storage.pushData")
                    .setSource(source)
                    .setMetaValue(FILE_NAME_KEY, fileName)
                    .setMetaValue(FILE_SIZE_KEY, fileSize);
        }

        public int getFileSize() {
            return meta().getInt(FILE_SIZE_KEY, 0);
        }

        public String getFileName() {
            return meta().getString(FILE_NAME_KEY);
        }

        @Override
        public String toString() {
            return String.format("(%s) [%s] : pushed numass data file with name '%s' and size '%d'",
                    time().toString(), sourceTag(), getFileName(), getFileSize());
        }

    }

}
