package inr.numass.storage;

import hep.dataforge.context.Context;
import hep.dataforge.data.DataFactory;
import hep.dataforge.data.DataFilter;
import hep.dataforge.data.DataTree;
import hep.dataforge.meta.Meta;
import hep.dataforge.storage.commons.StorageUtils;
import inr.numass.data.NumassData;

/**
 * Created by darksnake on 03-Feb-17.
 */
public class NumassDataFactory extends DataFactory<NumassData> {

    public NumassDataFactory() {
        super(NumassData.class);
    }

    @Override
    public String getName() {
        return "numass";
    }

    @Override
    protected void buildChildren(Context context, DataTree.Builder<NumassData> builder, DataFilter filter, Meta meta) {
        NumassStorage storage = NumassStorage.buildNumassRoot(
                meta.getString("path"),
                meta.getBoolean("readOnly", true),
                meta.getBoolean("monitor", false)
        );
        StorageUtils.loaderStream(storage).forEach(pair -> {
            if (pair.getValue() instanceof NumassData) {
                builder.putStatic(pair.getKey(), (NumassData) pair.getValue());
            }
        });
    }
}
