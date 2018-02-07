package inr.numass.data.storage;

import hep.dataforge.context.Context;
import hep.dataforge.data.DataFactory;
import hep.dataforge.data.DataTree;
import hep.dataforge.meta.Meta;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.commons.StorageManager;
import hep.dataforge.storage.commons.StorageUtils;
import inr.numass.data.api.NumassSet;

/**
 * Created by darksnake on 03-Feb-17.
 */
public class NumassDataFactory extends DataFactory<NumassSet> {

    public NumassDataFactory() {
        super(NumassSet.class);
    }

    @Override
    public String getName() {
        return "numass";
    }


    @Override
    protected void fill(DataTree.Builder<NumassSet> builder, Context context, Meta meta) {
        Meta newMeta = meta.getBuilder().setValue("type", "numass");
        Storage storage = context.load(StorageManager.class, Meta.empty()).buildStorage(newMeta);
        StorageUtils.loaderStream(storage).forEach(loader -> {
            if (loader instanceof NumassSet) {
                builder.putStatic(loader.getFullName().toUnescaped(), (NumassSet) loader);
            }
        });
    }
}
