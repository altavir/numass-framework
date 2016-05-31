/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.actions;

import hep.dataforge.actions.GenericAction;
import hep.dataforge.context.Context;
import hep.dataforge.data.Data;
import hep.dataforge.data.DataFilter;
import hep.dataforge.data.DataNode;
import hep.dataforge.data.DataSet;
import hep.dataforge.data.StaticData;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.description.ValueDef;
import hep.dataforge.exceptions.StorageException;
import hep.dataforge.meta.Meta;
import hep.dataforge.storage.api.Loader;
import hep.dataforge.storage.commons.StorageUtils;
import inr.numass.storage.NumassData;
import inr.numass.storage.NumassDataLoader;
import inr.numass.storage.NumassStorage;

/**
 *
 * @author Alexander Nozik
 */
@TypedActionDef(name = "readStorage",
        outputType = NumassData.class, info = "Read numass storage data")
@ValueDef(name = "uri", info = "The path to Numass storage")
//@NodeDef(name = "debunch", target = "class::inr.numass.actions.DebunchAction", info = "If given, governs debunching")
public class ReadNumassStorageAction extends GenericAction<Void, NumassData> {

    @Override
    public DataNode<NumassData> run(Context context, DataNode<Void> data, Meta actionMeta) {
        try {
            NumassStorage storage = NumassStorage.buildNumassRoot(actionMeta.getString("uri"), true, false);
            DataFilter filter = new DataFilter().configure(actionMeta);

            DataSet.Builder<NumassData> builder = DataSet.builder(NumassData.class);

            boolean forwardOnly = actionMeta.getBoolean("forwardOnly", false);
            boolean reverseOnly = actionMeta.getBoolean("reverseOnly", false);

            StorageUtils.loaderStream(storage).forEach(pair -> {
                Loader loader = pair.getValue();
                if (loader instanceof NumassData) {
                    NumassDataLoader nd = (NumassDataLoader) loader;
                    boolean reversed = nd.isReversed();
                    if ((reverseOnly && reversed) || (forwardOnly && !reversed) || (!forwardOnly && !reverseOnly)) {
                        Data<NumassData> datum = new StaticData<>(nd);
                        if (filter.acceptData(pair.getKey(), datum)) {
                            builder.putData(pair.getKey(), datum);
                        }
                    }
                }
            });
//            DataSet.Builder<NumassData> builder = DataSet.builder(NumassData.class);
//
//            StorageUtils.loaderStream(storage).forEach(pair -> {
//                Loader loader = pair.getValue();
//                if (loader instanceof NumassData) {
//                    Data<NumassData> datum = new StaticData<>((NumassData) loader);
//                    if (filter.acceptData(pair.getKey(), datum)) {
//                        builder.putData(pair.getKey(), datum);
//                    }
//                }
//            });

            storage.legacyFiles().forEach(nd -> {
                Data<NumassData> datum = new StaticData<>(nd);
                if (filter.acceptData(nd.getName(), datum)) {
                    builder.putData("legacy." + nd.getName(), datum);
                }
            });

            return builder.build();
        } catch (StorageException ex) {
            throw new RuntimeException("Failed to load storage", ex);
        }
    }

}
