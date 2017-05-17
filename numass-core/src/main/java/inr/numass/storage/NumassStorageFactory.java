package inr.numass.storage;

import hep.dataforge.context.Context;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.api.StorageType;

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

    @Override
    public String type() {
        return "numass";
    }

    @Override
    public Storage build(Context context, Meta meta) {
        return new NumassStorage(context, meta);
    }
}
