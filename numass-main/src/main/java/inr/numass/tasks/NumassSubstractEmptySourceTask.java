/*
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package inr.numass.tasks;

import hep.dataforge.data.Data;
import hep.dataforge.data.DataNode;
import hep.dataforge.data.DataTree;
import hep.dataforge.data.DataUtils;
import hep.dataforge.io.ColumnedDataWriter;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.tables.DataPoint;
import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.MapPoint;
import hep.dataforge.tables.Table;
import hep.dataforge.workspace.AbstractTask;
import hep.dataforge.workspace.TaskModel;

import java.io.OutputStream;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Created by darksnake on 06-Sep-16.
 */
public class NumassSubstractEmptySourceTask extends AbstractTask<Table> {
    @Override
    public String getName() {
        return "substractEmpty";
    }

    @Override
    protected DataNode<Table> run(TaskModel model, DataNode<?> data) {
        DataTree.Builder<Table> builder = DataTree.builder(Table.class);
        DataNode<Table> rootNode = data.getCheckedNode("prepare", Table.class);
        Data<? extends Table> emptySource = data.getCheckedNode("empty", Table.class).getData();
        rootNode.forEachDataWithType(Table.class, input -> {
            Data<? extends Table> res = subtractBackground(input, emptySource);
            res.getGoal().onComplete((r, err) -> {
                if (r != null) {
                    OutputStream out = model.getContext().io().out("merge", input.getName() + ".subtract");
                    ColumnedDataWriter.writeDataSet(out, r,
                            input.meta().getBuilder().setNode("empty", emptySource.meta()).toString());
                }
            });

            builder.putData(input.getName(), res);
        });
        return builder.build();
    }

    @Override
    protected TaskModel transformModel(TaskModel model) {
        Meta modelMeta = model.meta();
        model.dependsOn("prepare", modelMeta, "prepare");
        MetaBuilder emptyCfg = new MetaBuilder("prepare")
                .setNode(modelMeta.getMeta("prepare"))
                .setNode("data", modelMeta.getMeta("empty"))
                .setNode(new MetaBuilder("merge").setValue("mergeName", model.meta().getName() + ".empty"));
        model.dependsOn("prepare", emptyCfg, "empty");
        return model;
    }


    private Data<? extends Table> subtractBackground(Data<? extends Table> mergeData, Data<? extends Table> emptyData) {
        return DataUtils.combine(Table.class, mergeData, emptyData, mergeData.meta(), (BiFunction<Table, Table, Table>) this::subtractBackground);
    }

    private Table subtractBackground(Table merge, Table empty) {
        ListTable.Builder builder = new ListTable.Builder(merge.getFormat());
        merge.stream().forEach(point -> {
            MapPoint.Builder pointBuilder = new MapPoint.Builder(point);
            Optional<DataPoint> referencePoint = empty.stream()
                    .filter(p -> Math.abs(p.getDouble("Uset") - point.getDouble("Uset")) < 0.1).findFirst();
            if (referencePoint.isPresent()) {
                pointBuilder.putValue("CR", Math.max(0, point.getDouble("CR") - referencePoint.get().getDouble("CR")));
                pointBuilder.putValue("CRerr", Math.sqrt(Math.pow(point.getDouble("CRerr"), 2d) + Math.pow(referencePoint.get().getDouble("CRerr"), 2d)));
            } else {
                getLogger().warn("No reference point found for Uset = {}", point.getDouble("Uset"));
            }
            builder.row(pointBuilder.build());
        });

        return builder.build();
    }
}