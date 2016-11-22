/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.workspace;

import hep.dataforge.actions.Action;
import hep.dataforge.computation.ProgressCallback;
import hep.dataforge.context.Context;
import hep.dataforge.data.*;
import hep.dataforge.description.DescriptorBuilder;
import hep.dataforge.description.NodeDescriptor;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.Template;
import hep.dataforge.storage.api.Loader;
import hep.dataforge.storage.commons.StorageUtils;
import hep.dataforge.tables.Table;
import hep.dataforge.workspace.AbstractTask;
import hep.dataforge.workspace.TaskModel;
import inr.numass.actions.MergeDataAction;
import inr.numass.actions.MonitorCorrectAction;
import inr.numass.actions.PrepareDataAction;
import inr.numass.storage.NumassData;
import inr.numass.storage.NumassDataLoader;
import inr.numass.storage.NumassStorage;
import inr.numass.storage.SetDirectionUtility;

import java.net.URI;

/**
 * Prepare data task
 *
 * @author Alexander Nozik
 */
public class NumassPrepareTask extends AbstractTask<Table> {

    /*
        <action type="readStorage" uri="file://D:\Work\Numass\data\2016_04\T2_data\">
		<include pattern="Fill_2*"/>
		<exclude pattern="Fill_2_4_Empty*"/>
		<exclude pattern="Fill_2_1.set_1*"/>
		<exclude pattern="Fill_2_1.set_2*"/>
		<exclude pattern="Fill_2_1.set_3*"/>
		<exclude pattern="Fill_2_1.set_4*"/>
	</action>
	<action type="prepareData" lowerWindow="500" upperWindow="1700" deadTime="4.9534e-6 + 1.51139e-10*U">
		<underflow function = "1.0 + 15.216 * Math.exp( - U / 2477.46 )" threshold = "14000"/>
	</action>
	<action type="monitor" monitorPoint="${numass.monitorPoint}" monitorFile="${numass.setName}_monitor"/>
	<action type="merge" mergeName="${numass.setName}"/>
     */
    @Override
    protected DataNode<Table> run(TaskModel model, ProgressCallback callback, DataNode<?> input) {
        Meta config = model.meta();
        Context context = model.getContext();

        //acquiring initial data. Data node could not be empty
        Meta dataMeta = config.getMeta("data");
        URI storageUri = input.getCheckedData("dataRoot", URI.class).get();
        DataSet.Builder<NumassData> dataBuilder = readData(callback, context, storageUri, dataMeta);
//        if (config.hasMeta("empty")) {
//            dataBuilder.putNode("empty", readData(callback, context, storageUri, config.getMeta("empty")).build());
//        }

        DataNode<NumassData> data = dataBuilder.build();

        //preparing table data
        Meta prepareMeta = config.getMeta("prepare");
        DataNode<Table> tables = runAction(new PrepareDataAction(), callback, context, data, prepareMeta);

        if (config.hasMeta("monitor")) {
            Meta monitorMeta = config.getMeta("monitor");
            tables = runAction(new MonitorCorrectAction(), callback, context, tables, monitorMeta);
        }

        //merging if needed
        if (config.hasMeta("merge")) {
            DataTree.Builder<Table> resultBuilder = DataTree.builder(Table.class);
            DataTree.Builder<Table> tablesForMerge = new DataTree.Builder<>(tables);

//            //extracting empty data
//            if (config.hasMeta("empty")) {
//                DataNode<Table> emptySourceNode = tables.getCheckedNode("empty", Table.class);
//                Meta emptyMergeMeta = new MetaBuilder("emptySource").setValue("mergeName", "emptySource");
//                resultBuilder.putData("merge.empty", runAction(new MergeDataAction(), callback, context, emptySourceNode, emptyMergeMeta).getData());
//                tablesForMerge.removeNode("empty");
//            }

            config.getMetaList("merge").forEach(mergeNode -> {
                Meta mergeMeta = Template.compileTemplate(mergeNode, config);
                DataNode<Table> mergeData = runAction(new MergeDataAction(), callback, context, tablesForMerge.build(), mergeMeta);
                mergeData.dataStream().forEach(d -> {
                    resultBuilder.putData("merge." + d.getName(), d.anonymize());
                });
            });
            tables = resultBuilder.build();
        }

        return tables;
    }

    @Override
    protected TaskModel transformModel(TaskModel model) {
        String rootName = model.meta().getString("data.root", "dataRoot");
        model.data(rootName, "dataRoot");
        return model;
    }

    private DataSet.Builder<NumassData> readData(ProgressCallback callback, Context context, URI numassRoot, Meta meta) {

        NumassStorage storage = NumassStorage.buildNumassRoot(numassRoot, true, false);
        DataFilter filter = new DataFilter().configure(meta);

        boolean forwardOnly = meta.getBoolean("forwardOnly", false);
        boolean reverseOnly = meta.getBoolean("reverseOnly", false);
        SetDirectionUtility.load(context);

        DataSet.Builder<NumassData> builder = DataSet.builder(NumassData.class);
        callback.setMaxProgress(StorageUtils.loaderStream(storage).count());
        StorageUtils.loaderStream(storage).forEach(pair -> {
            Loader loader = pair.getValue();
            if (loader instanceof NumassData) {
                NumassDataLoader nd = (NumassDataLoader) loader;
                Data<NumassData> datum = Data.buildStatic(nd);
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

        if (meta.getBoolean("loadLegacy", false)) {
            storage.legacyFiles().forEach(nd -> {
                Data<NumassData> datum = Data.buildStatic(nd);
                if (filter.acceptData(nd.getName(), datum)) {
                    builder.putData("legacy." + nd.getName(), datum);
                }
            });
        }
        //FIXME remove in later revisions
        SetDirectionUtility.save(context);

        return builder;
    }

    private <T, R> DataNode<R> runAction(Action<T, R> action, ProgressCallback callback, Context context, DataNode<T> data, Meta meta) {
        return action.withContext(context).withParentProcess(callback.workName()).run(data, meta);
    }

    @Override
    public void validate(TaskModel model) {
        if (!model.meta().hasMeta("data")) {

        }
    }

    @Override
    public String getName() {
        return "prepare";
    }

    @Override
    public NodeDescriptor getDescriptor() {
        return new DescriptorBuilder(getName())
                .addNode("prepare", PrepareDataAction.class)
                .addNode("monitor", MonitorCorrectAction.class)
                .addNode("merge", MergeDataAction.class)
                .build();
    }
}
