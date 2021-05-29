/*
 * Copyright  2018 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package hep.dataforge.grind.workspace

import hep.dataforge.actions.Action
import hep.dataforge.actions.ActionEnv
import hep.dataforge.data.Data
import hep.dataforge.data.DataNode
import hep.dataforge.data.DataNodeBuilder
import hep.dataforge.data.DataSet
import hep.dataforge.meta.Meta
import hep.dataforge.workspace.tasks.AbstractTask
import hep.dataforge.workspace.tasks.Task
import hep.dataforge.workspace.tasks.TaskModel

/**
 * A collection of static methods to create tasks for WorkspaceSpec
 */
class DefaultTaskLib {

//    /**
//     * Create a task using
//     * @param parameters
//     * @param taskName
//     * @param cl
//     * @return
//     */
//    static Task template(Map parameters = [:],
//                         String taskName,
//                         @DelegatesTo(GrindMetaBuilder) Closure cl) {
//        Meta meta = Grind.buildMeta(parameters, taskName, cl);
//        Context context = parameters.getOrDefault("context", Global.instance());
//
//        return StreamSupport.stream(ServiceLoader.load(TaskTemplate).spliterator(), false)
//                .filter { it.name == meta.getName() }
//                .map { it.build(context, meta) }
//                .findFirst().orElseThrow { new NameNotFoundException("Task template with name $taskName not found") }
//    }

    /**
     * Create a task using {@ling TaskSpec}
     * @param taskName
     * @param cl
     * @return
     */
    static Task build(String taskName,
                      @DelegatesTo(value = GrindTaskBuilder, strategy = Closure.DELEGATE_ONLY) Closure closure) {
        def taskSpec = new GrindTaskBuilder(taskName);
        def code = closure.rehydrate(taskSpec, null, null)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code.call()
        return taskSpec.build();
    }

    /**
     * A task with single join action delegated to {@link hep.dataforge.workspace.tasks.KTaskBuilder#pipe}
     * @param params
     * @param name
     * @param action
     * @return
     */
    static Task pipe(String name, Map params = [:],
                     @DelegatesTo(value = ActionEnv, strategy = Closure.DELEGATE_ONLY) Closure action) {
        def builder = new GrindTaskBuilder(name)
        builder.model(params)
        builder.pipe { env ->
            def innerAction = action.rehydrate(env, null, null)
            innerAction.resolveStrategy = Closure.DELEGATE_ONLY;
            return { input -> innerAction.call(input) }
        }
        return builder.build()
    }

    /**
     * A task with single join action delegated to {@link hep.dataforge.workspace.tasks.KTaskBuilder#join}
     * @param params
     * @param name
     * @param action
     * @return
     */
    static Task join(String name, Map params = [:],
                     @DelegatesTo(value = ActionEnv, strategy = Closure.DELEGATE_FIRST) Closure action) {
        def builder = new GrindTaskBuilder(name)
        builder.model(params)
        builder.join { env ->
            def innerAction = action.rehydrate(env, null, null)
            innerAction.resolveStrategy = Closure.DELEGATE_ONLY;
            return { input -> innerAction.call(input) }
        }
        return builder.build()
    }

    /**
     * Create a task from single action using custom dependency builder
     * @param action
     * @return
     */
    static Task action(Action action, Map params = [:]) {
        def builder = new GrindTaskBuilder(action.name)
        builder.model(params)
        builder.action(action)
        return builder.build()
    }

    /**
     * Create a single action task using action class reference and custom dependency builder
     * @param action
     * @param dependencyBuilder
     * @return
     */
    static Task action(Class<Action> actionClass, Map params = [:]) {
        Action ac = actionClass.newInstance()
        return action(ac,params)
    }

    static class CustomTaskSpec {
        final TaskModel model
        final DataNode input
        final DataNodeBuilder result = DataSet.Companion.edit();

        CustomTaskSpec(TaskModel model, DataNode input) {
            this.model = model
            this.input = input
        }

        void yield(String name, Data data) {
            result.putData(name, data)
        }

        void yield(DataNode node) {
            result.putNode(node)
        }

    }

    static Task custom(Map parameters = [data: "*"], String name,
                       @DelegatesTo(value = CustomTaskSpec, strategy = Closure.DELEGATE_FIRST) Closure cl) {
        return new AbstractTask() {

            @Override
            Class getType() {
                return Object
            }

            @Override
            protected DataNode run(TaskModel model, DataNode dataNode) {
                CustomTaskSpec spec = new CustomTaskSpec(model, dataNode);
                Closure code = cl.rehydrate(spec, null, null)
                code.resolveStrategy = Closure.DELEGATE_ONLY
                code.call()
                return spec.result.build();
            }

            @Override
            protected void buildModel(TaskModel.Builder model, Meta meta) {
                dependencyBuilder(parameters).accept(model, meta)
            }

            @Override
            String getName() {
                return name
            }
        }
    }

    /**
     * Execute external process task
     * @param parameters
     * @param name the name of the task
     * @return
     */
    static Task exec(String name, Map params = [:],
                     @DelegatesTo(value = ExecSpec, strategy = Closure.DELEGATE_ONLY) Closure cl) {
        ExecSpec spec = new ExecSpec();
        spec.actionName = name;
        Closure script = cl.rehydrate(spec, null, null)
        script.setResolveStrategy(Closure.DELEGATE_ONLY)
        script.call()

        Action execAction = spec.build();
        return action(execAction, params)
    }
}
