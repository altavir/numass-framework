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
package hep.dataforge.actions;

import hep.dataforge.context.Context;
import hep.dataforge.data.Data;
import hep.dataforge.data.DataNode;
import hep.dataforge.data.NamedData;
import hep.dataforge.goals.AbstractGoal;
import hep.dataforge.goals.Goal;
import hep.dataforge.meta.Laminate;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.names.Name;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Action with multiple input data pieces but single output
 *
 * @param <T>
 * @param <R>
 * @author Alexander Nozik
 */
public abstract class ManyToOneAction<T, R> extends GenericAction<T, R> {

    public ManyToOneAction(@NotNull String name, @NotNull Class<T> inputType, @NotNull Class<R> outputType) {
        super(name, inputType, outputType);
    }

    @Override
    @NotNull
    public DataNode<R> run(Context context, DataNode<? extends T> set, Meta actionMeta) {
        checkInput(set);
        List<DataNode<T>> groups = buildGroups(context, (DataNode<T>) set, actionMeta);
        return wrap(getResultName(set.getName(), actionMeta), set.getMeta(), groups.stream().map(group->runGroup(context, group,actionMeta)));
    }

    public ActionResult<R> runGroup(Context context, DataNode<T> data, Meta actionMeta) {
        Meta outputMeta = outputMeta(data).build();
        Goal<R> goal = new ManyToOneGoal(context, data, actionMeta, outputMeta);
        String resultName = data.getName() == null ? getName() : data.getName();

        return new ActionResult<>(resultName, getOutputType(), goal, outputMeta, context.getHistory().getChronicle(resultName));
    }

    protected List<DataNode<T>> buildGroups(Context context, DataNode<T> input, Meta actionMeta) {
        return GroupBuilder.byMeta(inputMeta(context, input.getMeta(), actionMeta)).group(input);
    }

    /**
     * Perform actual calculation
     *
     * @param nodeName
     * @param input
     * @param meta
     * @return
     */
    protected abstract R execute(Context context, String nodeName, Map<String, T> input, Laminate meta);

    /**
     * Build output meta for resulting object
     *
     * @param input
     * @return
     */
    protected MetaBuilder outputMeta(DataNode<T> input) {
        MetaBuilder builder = new MetaBuilder(MetaBuilder.DEFAULT_META_NAME)
                .putValue("name", input.getName())
                .putValue("type", input.getType().getName());
        input.dataStream().forEach((NamedData<? extends T> data) -> {
            MetaBuilder dataNode = new MetaBuilder("data")
                    .putValue("name", data.getName());
            if (!data.getType().equals(input.getType())) {
                dataNode.putValue("type", data.getType().getName());
            }
//            if (!data.meta().isEmpty()) {
//                dataNode.putNode(DataFactory.NODE_META_KEY, data.meta());
//            }
            builder.putNode(dataNode);
        });
        return builder;
    }

    /**
     * An action to be performed before each group evaluation
     *
     * @param input
     */
    protected void beforeGroup(Context context, DataNode<? extends T> input) {

    }

    /**
     * An action to be performed after each group evaluation
     *
     * @param output
     */
    protected void afterGroup(Context context, String groupName, Meta outputMeta, R output) {

    }

    /**
     * Action  goal {@code fainOnError()} delegate
     *
     * @return
     */
    protected boolean failOnError() {
        return true;
    }

    private class ManyToOneGoal extends AbstractGoal<R> {

        private final Context context;
        private final DataNode<T> data;
        private final Meta actionMeta;
        private final Meta outputMeta;

        public ManyToOneGoal(Context context, DataNode<T> data, Meta actionMeta, Meta outputMeta) {
            super(getExecutorService(context, actionMeta));
            this.context = context;
            this.data = data;
            this.actionMeta = actionMeta;
            this.outputMeta = outputMeta;
        }

        @Override
        protected boolean failOnError() {
            return ManyToOneAction.this.failOnError();
        }

        @Override
        public Stream<Goal<?>> dependencies() {
            return data.nodeGoal().dependencies();
        }

        @Override
        protected R compute() throws Exception {
            Laminate meta = inputMeta(context, data.getMeta(), actionMeta);
            Thread.currentThread().setName(Name.Companion.joinString(getThreadName(actionMeta), data.getName()));
            beforeGroup(context, data);
            // In this moment, all the data is already calculated
            Map<String, T> collection = data.dataStream()
                    .filter(Data::isValid) // filter valid data only
                    .collect(Collectors.toMap(NamedData::getName, Data::get));
            R res = execute(context, data.getName(), collection, meta);
            afterGroup(context, data.getName(), outputMeta, res);
            return res;
        }

    }

}
