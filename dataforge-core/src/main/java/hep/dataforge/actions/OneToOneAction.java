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
import hep.dataforge.context.Global;
import hep.dataforge.data.DataNode;
import hep.dataforge.data.NamedData;
import hep.dataforge.goals.PipeGoal;
import hep.dataforge.io.history.Chronicle;
import hep.dataforge.meta.Laminate;
import hep.dataforge.meta.Meta;
import hep.dataforge.names.Name;
import kotlin.Pair;
import org.slf4j.Logger;

/**
 * A template to builder actions that reflect strictly one to one content
 * transformations
 *
 * @param <T>
 * @param <R>
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public abstract class OneToOneAction<T, R> extends GenericAction<T, R> {

    public OneToOneAction(String name, Class<T> inputType, Class<R> outputType) {
        super(name,inputType,outputType);
    }



    @Override
    public DataNode<R> run(Context context, DataNode<? extends T> set, Meta actionMeta) {
        checkInput(set);
        if (set.isEmpty()) {
            throw new RuntimeException(getName() + ": Running 1 to 1 action on empty data node");
        }

        return wrap(
                set.getName(),
                set.getMeta(),
                set.dataStream(true).map(data -> runOne(context, data, actionMeta))
        );
    }

    /**
     * Build asynchronous result for single data. Data types separated from
     * action generics to be able to operate maps instead of raw data
     *
     * @param data
     * @param actionMeta
     * @return
     */
    protected ActionResult<R> runOne(Context context, NamedData<? extends T> data, Meta actionMeta) {
        if (!this.getInputType().isAssignableFrom(data.getType())) {
            throw new RuntimeException(String.format("Type mismatch in action %s. %s expected, but %s recieved",
                    getName(), getInputType().getName(), data.getType().getName()));
        }

        Pair<String, Meta> resultParamters = outputParameters(context, data, actionMeta);

        Laminate meta = inputMeta(context, data.getMeta(), actionMeta);
        String resultName = resultParamters.getFirst();
        Meta outputMeta = resultParamters.getSecond();

        PipeGoal<? extends T, R> goal = new PipeGoal<>(getExecutorService(context, meta), data.getGoal(),
                input -> {
                    Thread.currentThread().setName(Name.Companion.joinString(getThreadName(actionMeta), resultName));
                    return transform(context, resultName, input, meta);
                }
        );
        return new ActionResult<>(resultName, getOutputType(), goal, outputMeta, context.getHistory().getChronicle(resultName));
    }

    protected Chronicle getLog(Context context, String dataName) {
        return context.getHistory().getChronicle(Name.Companion.joinString(dataName, getName()));
    }

    /**
     * @param name      name of the input item
     * @param input     input data
     * @param inputMeta combined meta for this evaluation. Includes data meta,
     *                  group meta and action meta
     * @return
     */
    private R transform(Context context, String name, T input, Laminate inputMeta) {
        beforeAction(context, name, input, inputMeta);
        R res = execute(context, name, input, inputMeta);
        afterAction(context, name, res, inputMeta);
        return res;
    }

    /**
     * Utility method to run action outside of context or execution chain
     *
     * @param input
     * @param metaLayers
     * @return
     */
    public R simpleRun(T input, Meta... metaLayers) {
        return transform(Global.INSTANCE, "simpleRun", input, inputMeta(Global.INSTANCE, metaLayers));
    }

    protected abstract R execute(Context context, String name, T input, Laminate meta);

    /**
     * Build output meta for given data. This meta is calculated on action call
     * (no lazy calculations). By default output meta is the same as input data
     * meta.
     *
     * @param actionMeta
     * @param data
     * @return
     */
    protected Pair<String, Meta> outputParameters(Context context, NamedData<? extends T> data, Meta actionMeta) {
        return new Pair<>(getResultName(data.getName(), actionMeta), data.getMeta());
    }

    protected void afterAction(Context context, String name, R res, Laminate meta) {
        Logger logger = getLogger(context, meta);
        if (res == null) {
            logger.error("Action {} returned 'null' on data {}", getName(), name);
            throw new RuntimeException("Null result of action");//TODO add emty data to handle this
        }
        logger.debug("Action '{}[{}]' is finished", getName(), name);
    }

    protected void beforeAction(Context context, String name, T datum, Laminate meta) {
        if (context.getBoolean("actions.reportStart", false)) {
            report(context, name, "Starting action {} on data with name {} with following configuration: \n\t {}", getName(), name, meta.toString());
        }
        getLogger(context, meta).debug("Starting action '{}[{}]'", getName(), name);
    }

}
