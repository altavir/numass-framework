/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.tasks;

import hep.dataforge.actions.GenericAction;
import hep.dataforge.context.Context;
import hep.dataforge.data.DataFilter;
import hep.dataforge.data.DataNode;
import hep.dataforge.data.DataTree;
import hep.dataforge.description.NodeDef;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.Template;
import hep.dataforge.tables.Table;
import hep.dataforge.workspace.AbstractTask;
import hep.dataforge.workspace.TaskModel;
import inr.numass.actions.AnalyzeDataAction;
import inr.numass.actions.MergeDataAction;
import inr.numass.actions.MonitorCorrectAction;
import inr.numass.actions.TransformDataAction;
import inr.numass.data.api.NumassSet;

/**
 * Prepare data task
 *
 * @author Alexander Nozik
 */
@NodeDef(name = "prepare")
@NodeDef(name = "monitor")
@NodeDef(name = "merge")
@NodeDef(name = "data")
public class NumassPrepareTask extends AbstractTask<Table> {

    @Override
    protected DataNode<Table> run(TaskModel model, DataNode<?> input) {
        Meta config = model.meta();
        Context context = model.getContext();

        //acquiring initial data. Data node could not be empty

        DataFilter filter = new DataFilter().configure(config.getMeta("data"));

        DataNode<NumassSet> data = filter.filter(input.checked(NumassSet.class));

//        Meta dataMeta = config.getMeta("data");
//        URI storageUri = input.getCheckedData("dataRoot", URI.class).get();
//        DataSet.Builder<NumassData> dataBuilder = readData(getWork(model, input.getName()), context, storageUri, dataMeta);
//        DataNode<NumassData> data = dataBuilder.build();

        //preparing table data
        Meta prepareMeta = config.getMeta("prepare");
        DataNode<Table> tables = runAction(new AnalyzeDataAction(), context, data, prepareMeta);

        tables = runAction(new TransformDataAction(), context, tables, prepareMeta);

        if (config.hasMeta("monitor")) {
            Meta monitorMeta = config.getMeta("monitor");
            tables = runAction(new MonitorCorrectAction(), context, tables, monitorMeta);
        }

        //merging if needed
        if (config.hasMeta("merge")) {
            DataTree.Builder<Table> resultBuilder = DataTree.builder(Table.class);
            DataTree.Builder<Table> tablesForMerge = new DataTree.Builder<>(tables);


            config.getMetaList("merge").forEach(mergeNode -> {
                Meta mergeMeta = Template.compileTemplate(mergeNode, config);
                DataNode<Table> mergeData = runAction(new MergeDataAction(), context, tablesForMerge.build(), mergeMeta);
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
        if (model.hasValue("data.from")) {
            model.data(model.getString("data.from.*"));
        } else {
            model.data("*");
        }
        return model;
    }

//    private DataSet.Builder<NumassData> readData(Work callback, Context context, URI numassRoot, Meta meta) {
//
//        NumassStorage storage = NumassStorage.buildNumassRoot(numassRoot, true, false);
//        DataFilter filter = new DataFilter().configure(meta);
//
//        boolean forwardOnly = meta.getBoolean("forwardOnly", false);
//        boolean reverseOnly = meta.getBoolean("reverseOnly", false);
////        SetDirectionUtility.load(context);
//
//        DataSet.Builder<NumassData> builder = DataSet.builder(NumassData.class);
//        callback.setMaxProgress(StorageUtils.loaderStream(storage).count());
//        StorageUtils.loaderStream(storage).forEach(pair -> {
//            Loader loader = pair.getValue();
//            if (loader instanceof NumassDataLoader) {
//                NumassDataLoader nd = (NumassDataLoader) loader;
//                Data<NumassData> datum = buildData(context, nd, meta);
//                if (filter.acceptData(pair.getKey(), datum)) {
//                    boolean accept = true;
//                    if (forwardOnly || reverseOnly) {
//                        boolean reversed = nd.isReversed();
//                        accept = (reverseOnly && reversed) || (forwardOnly && !reversed);
//                    }
//                    if (accept) {
//                        builder.putData(pair.getKey(), datum);
//                    }
//                }
//            }
//            callback.increaseProgress(1d);
//        });
//
//        if (meta.getBoolean("loadLegacy", false)) {
//            storage.legacyFiles().forEach(nd -> {
//                Data<NumassData> datum = Data.buildStatic(nd);
//                if (filter.acceptData(nd.getName(), datum)) {
//                    builder.putData("legacy." + nd.getName(), datum);
//                }
//            });
//        }
//        //FIXME remove in later revisions
////        SetDirectionUtility.save(context);
//
//        return builder;
//    }

//    private Data<NumassData> buildData(Context context, NumassDataLoader loader, Meta meta) {
//        if (meta.hasNode("debunch")) {
//            return Data.buildStatic(loader.applyRawTransformation(rp -> debunch(context, rp, meta.getMeta("debunch"))));
//        } else {
//            return Data.buildStatic(loader);
//        }
//    }
//
//    private NMPoint debunch(Context context, RawNMPoint point, Meta meta) {
//        int upper = meta.getInt("upperchanel", RawNMPoint.MAX_CHANEL);
//        int lower = meta.getInt("lowerchanel", 0);
//        double rejectionprob = meta.getDouble("rejectprob", 1e-10);
//        double framelength = meta.getDouble("framelength", 1);
//        double maxCR = meta.getDouble("maxcr", 500d);
//
//        double cr = point.selectChanels(lower, upper).getCr();
//        if (cr < maxCR) {
//            DebunchReport report = new FrameAnalizer(rejectionprob, framelength, lower, upper).debunchPoint(point);
//            return new NMPoint(report.getPoint());
//        } else {
//            return new NMPoint(point);
//        }
//    }

    private <T, R> DataNode<R> runAction(GenericAction<T, R> action, Context context, DataNode<T> data, Meta meta) {
        return action.run(context, data, meta);
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

//    @Override
//    public NodeDescriptor getDescriptor() {
//        return new DescriptorBuilder(getName())
//                .addNode("prepare", PrepareDataAction.class)
//                .addNode("monitor", MonitorCorrectAction.class)
//                .addNode("merge", MergeDataAction.class)
//                .build();
//    }
}
