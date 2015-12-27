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

import hep.dataforge.events.Event;
import hep.dataforge.exceptions.StorageException;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.storage.filestorage.FileStorage;
import hep.dataforge.storage.filestorage.VFSUtils;
import inr.numass.data.NMFile;
import inr.numass.data.NumassData;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import static org.apache.commons.vfs2.FileType.FOLDER;
import org.slf4j.LoggerFactory;

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
    public static final String GROUP_META_FILE = "numass_group_meta";

    /**
     * Create root numass storage
     *
     * @param dir
     * @param readOnly
     * @return
     * @throws StorageException
     */
    public static NumassStorage buildLocalNumassRoot(File dir, boolean readOnly) throws StorageException {
        try {
            Meta meta = new MetaBuilder("storage")
                    .setValue("type", "file.numass")
                    .setValue("readOnly", readOnly)
                    .setValue("monitor", false);
            return new NumassStorage(VFSUtils.getLocalFile(dir), meta);
        } catch (FileSystemException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static NumassStorage buildRemoteNumassRoot(String ip, int port, String login, String password, String path) throws StorageException {
        try {
            Meta meta = new MetaBuilder("storage")
                    .setValue("type", "file.numass")
                    .setValue("readOnly", true)
                    .setValue("monitor", false);            
            return new NumassStorage(VFSUtils.getRemoteFile(ip, port, login, password, path), meta);
        } catch (FileSystemException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static NumassStorage buildNumassRoot(String uri, boolean readOnly, boolean monitor) throws StorageException {
        try {
            Meta meta = new MetaBuilder("storage")
                    .setValue("type", "file.numass")
                    .setValue("readOnly", readOnly)
                    .setValue("monitor", monitor);           
            return new NumassStorage(VFSUtils.getRemoteFile(uri), meta);
        } catch (FileSystemException ex) {
            throw new RuntimeException(ex);
        }
    }

    public NumassStorage(FileStorage parent, String path, Meta config) throws StorageException {
        super(parent, path, config);
        super.refresh();
        //TODO read meta from numass_group_meta to .numass element
    }

    protected NumassStorage(FileObject dir, Meta config) throws StorageException {
        super(dir, config);
        super.refresh();
    }

//    protected NumassStorage(FileObject dir, boolean readOnly) throws StorageException {
//        super(dir, null);
//        super.setReadOnly(readOnly);
//        super.refresh();
//    }
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
     * @param path
     * @param fileName
     * @param stream
     * @param size
     */
    @SuppressWarnings("unchecked")
    public void pushNumassData(String fileName, ByteBuffer data) throws StorageException {
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
            getDefaultEventLoader().push(new NumassDataPointEvent(getName(), fileName, (int) nmFile.getContent().getSize()));
        } catch (IOException ex) {
            throw new StorageException(ex);
        }
    }

    @Override
    public NumassStorage buildShelf(String path, Meta an) throws StorageException {

        //TODO add recusive shelves builders for composite paths
        //converting dataforge paths to file paths
        path = path.replace('.', File.separatorChar);
        return new NumassStorage(this, path, an);
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

    public static class NumassDataPointEvent implements Event {

        private final String source;
        private final String fileName;
        private final int fileSize;
        private final Instant time = Instant.now();

        public NumassDataPointEvent(String source, String fileName, int fileSize) {
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.source = source;
        }

        @Override
        public int priority() {
            return 0;
        }

        @Override
        public String type() {
            return "numass.storage.pushData";
        }

        @Override
        public String source() {
            return source;
        }

        @Override
        public Instant time() {
            return time;
        }

        public int getFileSize() {
            return fileSize;
        }

        public String getFileName() {
            return fileName;
        }

        @Override
        public String toString() {
            return String.format("(%s) [%s] : pushed numass data file with name '%s' and size '%d'",
                    time().toString(), source(), getFileName(), getFileSize());
        }

    }

}
