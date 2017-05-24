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

import hep.dataforge.context.Context;
import hep.dataforge.events.Event;
import hep.dataforge.events.EventBuilder;
import hep.dataforge.exceptions.StorageException;
import hep.dataforge.meta.Meta;
import hep.dataforge.storage.filestorage.FilePointLoader;
import hep.dataforge.storage.filestorage.FileStorage;
import inr.numass.data.NMFile;
import inr.numass.data.NumassData;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.vfs2.FileType.FOLDER;

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
            for (FileObject file : getDataDir().getChildren()) {
                try {
                    if (file.getType() == FOLDER) {
                        FileObject meta = file.resolveFile(NumassDataLoader.META_FRAGMENT_NAME);
                        if (meta.exists()) {
                            this.loaders.put(file.getName().getBaseName(),
                                    NumassDataLoader.fromDir(this, file, null));
                        } else {
                            this.shelves.put(file.getName().getBaseName(),
                                    new NumassStorage(this, file.getName().getBaseName(), meta()));
                        }
                    } else if (file.getName().toString().endsWith(NUMASS_ZIP_EXTENSION)) {
                        this.loaders.put(file.getName().getBaseName(), NumassDataLoader.fromZip(this, file));
                    } else if (file.getName().toString().endsWith(".points")) {
                        try {
                            loaders.put(FilenameUtils.getBaseName(file.getName().getBaseName()),
                                    FilePointLoader.fromFile(this, file, true));
                        } catch (Exception ex) {
                            getContext().getLogger().error("Failed to build numass point loader from file {}", file.getName());
                        }
                    } else {
                        //updating non-numass loader files
                        updateFile(file);
                    }
                } catch (IOException ex) {
                    LoggerFactory.getLogger(getClass()).error("Error while creating numass loader", ex);
                } catch (StorageException ex) {
                    LoggerFactory.getLogger(getClass()).error("Error while creating numass group", ex);
                }
            }
        } catch (FileSystemException ex) {
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
            FileObject nmFile = getDataDir().resolveFile(fileName + NUMASS_ZIP_EXTENSION);
            if (!nmFile.exists()) {
                nmFile.createFile();
            } else {
                LoggerFactory.getLogger(getClass()).warn("Trying to rewrite existing numass data file {}", nmFile.toString());
            }
            try (OutputStream os = nmFile.getContent().getOutputStream(false)) {
                os.write(data.array());
            }
            dispatchEvent(NumassDataPointEvent.build(getName(), fileName, (int) nmFile.getContent().getSize()));
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
    public List<NumassData> legacyFiles() {
        try {
            List<NumassData> files = new ArrayList<>();
            for (FileObject file : getDataDir().getChildren()) {
                if (file.getType() == FileType.FILE && file.getName().getExtension().equalsIgnoreCase("dat")) {
                    InputStream is = file.getContent().getInputStream();
                    String name = file.getName().getBaseName();
                    try {
                        files.add(NMFile.readStream(is, name, Meta.buildEmpty("numassData")));
                    } catch (Exception ex) {
                        LoggerFactory.getLogger(getClass()).error("Error while reading legacy numass file " + file.getName().getBaseName(), ex);
                    }
                }
            }
            return files;
        } catch (FileSystemException ex) {
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
