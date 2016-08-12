/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.tasks;

import hep.dataforge.actions.Action;
import hep.dataforge.computation.WorkManager;
import hep.dataforge.context.Context;
import hep.dataforge.data.DataNode;
import hep.dataforge.data.DataTree;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.Template;
import hep.dataforge.tables.Table;
import hep.dataforge.tables.TransformTableAction;
import hep.dataforge.workspace.GenericTask;
import hep.dataforge.workspace.TaskModel;
import hep.dataforge.workspace.TaskState;
import inr.numass.actions.MergeDataAction;
import inr.numass.actions.MonitorCorrectAction;
import inr.numass.actions.PrepareDataAction;
import inr.numass.actions.ReadNumassStorageAction;
import inr.numass.storage.NumassData;

/**
 * Prepare data task
 *
 * @author Alexander Nozik
 */
public class NumassPrepareTask extends GenericTask {

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
    @SuppressWarnings("unchecked")
    protected void transform(WorkManager.Callback callback, Context context, TaskState state, Meta config) {
        //acquiring initial data. Data node could not be empty
        Meta dataMeta = Template.compileTemplate(config.getNode("data"), config);
        DataNode<NumassData> data = runAction(new ReadNumassStorageAction(), callback, context, DataNode.empty(), dataMeta);
        state.setData("data", data);
        //preparing table data
        Meta prepareMeta = Template.compileTemplate(config.getNode("prepare"), config);
        DataNode<Table> tables = runAction(new PrepareDataAction(), callback, context, data, prepareMeta);
        state.setData("prepare", tables);

        if (config.hasNode("monitor")) {
            Meta monitorMeta = Template.compileTemplate(config.getNode("monitor"), config);
            tables = runAction(new MonitorCorrectAction(), callback, context, tables, monitorMeta);
            state.setData("monitor", tables);
        }

        //merging if needed
        if (config.hasNode("merge")) {
            DataTree.Builder resultBuilder = DataTree.builder(Table.class);
//            tables.dataStream().forEach(d -> resultBuilder.putData(d));
            DataNode<Table> finalTables = tables;
            config.getNodes("merge").forEach(mergeNode -> {
                Meta mergeMeta = Template.compileTemplate(mergeNode, config);
                DataNode<Table> mergeData = runAction(new MergeDataAction(), callback, context, finalTables, mergeMeta);
                mergeData.dataStream().forEach(d -> {
                    resultBuilder.putData("merge." + d.getName(), d.anonymize());
                });
            });
            tables = resultBuilder.build();
        }

        if (config.hasNode("transform")) {
            Meta filterMeta = Template.compileTemplate(config.getNode("transform"), config);
            tables = runAction(new TransformTableAction(), callback, context, tables, filterMeta);
        }

        state.finish(tables);
    }

    private <T, R> DataNode<R> runAction(Action<T, R> action, WorkManager.Callback callback, Context context, DataNode<T> data, Meta meta) {
        return action.withContext(context).withParentProcess(callback.workName()).run(data, meta);
    }

    @Override
    public void validate(TaskModel model) {
        if (!model.meta().hasNode("data")) {

        }
    }

    @Override
    public String getName() {
        return "numass.prepare";
    }

}
