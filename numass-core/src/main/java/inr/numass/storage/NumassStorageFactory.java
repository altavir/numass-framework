package inr.numass.storage;

import hep.dataforge.context.Context;
import hep.dataforge.meta.Meta;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.api.StorageType;

/**
 * Created by darksnake on 17-May-17.
 */
public class NumassStorageFactory implements StorageType {
//    /**
//     * Create root numass storage
//     *
//     * @param dir
//     * @param readOnly
//     * @return
//     * @throws StorageException
//     */
//    public static NumassStorage buildLocalNumassRoot(File dir, boolean readOnly, boolean monitor) throws StorageException {
//        try {
//            Meta meta = new MetaBuilder("storage")
//                    .setValue("type", "file.numass")
//                    .setValue("readOnly", readOnly)
//                    .setValue("monitor", monitor);
//            return new NumassStorage(VFSUtils.getLocalFile(dir), meta);
//        } catch (FileSystemException ex) {
//            throw new RuntimeException(ex);
//        }
//    }
//
//    public static NumassStorage buildLocalNumassRoot(File dir, boolean readOnly) throws StorageException {
//        return buildLocalNumassRoot(dir, readOnly, false);
//    }
//
//    public static NumassStorage buildRemoteNumassRoot(String ip, int port, String login, String password, String path) throws StorageException {
//        try {
//            Meta meta = new MetaBuilder("storage")
//                    .setValue("type", "file.numass")
//                    .setValue("readOnly", true)
//                    .setValue("monitor", false);
//            return new NumassStorage(VFSUtils.getRemoteFile(ip, port, login, password, path), meta);
//        } catch (FileSystemException ex) {
//            throw new RuntimeException(ex);
//        }
//    }
//
//    public static NumassStorage buildNumassStorage(FileStorage parent, String path, boolean readOnly, boolean monitor) throws StorageException {
//        Meta meta = new MetaBuilder("storage")
//                .setValue("type", "file.numass")
//                .setValue("readOnly", readOnly)
//                .setValue("monitor", monitor);
//        return new NumassStorage(parent, path, meta);
//    }
//
//    public static NumassStorage buildNumassRoot(String uri, boolean readOnly, boolean monitor) {
//        try {
//            Meta meta = new MetaBuilder("storage")
//                    .setValue("type", "file.numass")
//                    .setValue("readOnly", readOnly)
//                    .setValue("monitor", monitor);
//            return new NumassStorage(VFSUtils.getFile(uri), meta);
//        } catch (Exception ex) {
//            throw new RuntimeException(ex);
//        }
//    }
//
//    public static NumassStorage buildNumassRoot(URI uri, boolean readOnly, boolean monitor) {
//        try {
//            Meta meta = new MetaBuilder("storage")
//                    .setValue("type", "file.numass")
//                    .setValue("readOnly", readOnly)
//                    .setValue("monitor", monitor);
//            return new NumassStorage(VFSUtils.getFile(uri), meta);
//        } catch (Exception ex) {
//            throw new RuntimeException(ex);
//        }
//    }


    @Override
    public String type() {
        return "numass";
    }

    @Override
    public Storage build(Context context, Meta meta) {
        return new NumassStorage(context, meta);
    }
}
