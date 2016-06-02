/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.actions;

import hep.dataforge.actions.GenericAction;
import hep.dataforge.context.Context;
import hep.dataforge.context.DFProcess;
import hep.dataforge.context.ProcessManager.Callback;
import hep.dataforge.data.Data;
import hep.dataforge.data.DataFilter;
import hep.dataforge.data.DataNode;
import hep.dataforge.data.DataSet;
import hep.dataforge.data.StaticData;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.description.ValueDef;
import hep.dataforge.meta.Meta;
import hep.dataforge.storage.api.Loader;
import hep.dataforge.storage.commons.StorageUtils;
import inr.numass.storage.NumassData;
import inr.numass.storage.NumassDataLoader;
import inr.numass.storage.NumassStorage;
import inr.numass.storage.SetDirectionUtility;

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

            boolean forwardOnly = actionMeta.getBoolean("forwardOnly", false);
            boolean reverseOnly = actionMeta.getBoolean("reverseOnly", false);

            DFProcess<DataSet<NumassData>> process = context.processManager().<DataSet<NumassData>>post(getName(), (Callback callback) -> {
                //FIXME remove in later revisions
                SetDirectionUtility.load(context);

                DataSet.Builder<NumassData> builder = DataSet.builder(NumassData.class);
                callback.setMaxProgress(StorageUtils.loaderStream(storage).count());
                StorageUtils.loaderStream(storage).forEach(pair -> {
                    Loader loader = pair.getValue();
                    if (loader instanceof NumassData) {
                        NumassDataLoader nd = (NumassDataLoader) loader;
                        Data<NumassData> datum = new StaticData<>(nd);
                        if (filter.acceptData(pair.getKey(), datum)) {
                            boolean accept = true;
                            if (forwardOnly || reverseOnly) {
                                boolean reversed = nd.isReversed();
                                accept = (reverseOnly && reversed) || (forwardOnly && !reversed);
                            }
                            if (accept) {
                                builder.putData(pair.getKey(), datum);
                            }
                        }
                    }
                    callback.increaseProgress(1d);
                });

                if (actionMeta.getBoolean("loadLegacy", false)) {
                    logger().info("Loading legacy files");
                    storage.legacyFiles().forEach(nd -> {
                        Data<NumassData> datum = new StaticData<>(nd);
                        if (filter.acceptData(nd.getName(), datum)) {
                            builder.putData("legacy." + nd.getName(), datum);
                        }
                    });
                }
                //FIXME remove in later revisions
                SetDirectionUtility.save(context);

                return builder.build();
            });

            return process.getTask().get();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load storage", ex);
        }
    }

}
