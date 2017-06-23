package inr.numass.storage;

import hep.dataforge.context.Context;
import hep.dataforge.context.Global;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.api.StorageType;

import java.io.File;

/**
 * Created by darksnake on 17-May-17.
 */
public class NumassStorageFactory implements StorageType {

    public static MetaBuilder buildStorageMeta(String path, boolean readOnly, boolean monitor){
        return new MetaBuilder("storage")
                .setValue("path", path)
                .setValue("type", "numass")
                .setValue("readOnly", readOnly)
                .setValue("monitor", monitor);
    }

    /**
     * Build local storage with Global context. Used for tests.
     * @param file
     * @return
     */
    public static NumassStorage buildLocal(File file) {
        return new NumassStorage(Global.instance(),
                new MetaBuilder("storage").setValue("path", file.toURI()));
    }

    @Override
    public String type() {
        return "numass";
    }

    @Override
    public Storage build(Context context, Meta meta) {
        return new NumassStorage(context, meta);
    }
}
