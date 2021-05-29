/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.actions;

import hep.dataforge.context.BasicPlugin;
import hep.dataforge.context.Plugin;
import hep.dataforge.context.PluginDef;
import hep.dataforge.context.PluginFactory;
import hep.dataforge.meta.Meta;
import hep.dataforge.providers.Provides;
import hep.dataforge.providers.ProvidesNames;
import hep.dataforge.tables.ReadPointSetAction;
import hep.dataforge.tables.TransformTableAction;
import hep.dataforge.utils.Optionals;
import hep.dataforge.workspace.tasks.Task;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A support manager to dynamically load actions and tasks into the context
 *
 * @author Alexander Nozik
 */
@PluginDef(name = "actions", group = "hep.dataforge", info = "A list of available actions and task for given context")
public class ActionManager extends BasicPlugin {
    private final Map<String, Action> actionMap = new HashMap<>();
    private final Map<String, Task> taskMap = new HashMap<>();

    public ActionManager() {
        //TODO move somewhere else
        putAction(TransformTableAction.class);
        putAction(ReadPointSetAction.class);
    }

    protected Optional<ActionManager> getParent() {
        if (getContext().getParent() == null) {
            return Optional.empty();
        } else {
            return getContext().getParent().provide("actions", ActionManager.class);
        }
    }

    @Provides(Action.ACTION_TARGET)
    public Optional<Action> optAction(String name) {
        return Optionals.either(actionMap.get(name))
                .or(getParent().flatMap(parent -> parent.optAction(name)))
                .opt();
    }

    @Provides(Task.TASK_TARGET)
    public Optional<Task> optTask(String name) {
        return Optionals.either(taskMap.get(name))
                .or(getParent().flatMap(parent -> parent.optTask(name)))
                .opt();
    }

    public void put(Action action) {
        if (actionMap.containsKey(action.getName())) {
            LoggerFactory.getLogger(getClass()).warn("Duplicate action names in ActionManager.");
        } else {
            actionMap.put(action.getName(), action);
        }
    }

    public void put(Task task) {
        if (taskMap.containsKey(task.getName())) {
            LoggerFactory.getLogger(getClass()).warn("Duplicate task names in ActionManager.");
        } else {
            taskMap.put(task.getName(), task);
        }
    }

    /**
     * Put a task into the manager using action construction by reflections. Action must have empty constructor
     *
     * @param actionClass a {@link java.lang.Class} object.
     */
    public final void putAction(Class<? extends Action> actionClass) {
        try {
            put(actionClass.newInstance());
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("Action must have default empty constructor to be registered.");
        } catch (InstantiationException ex) {
            throw new RuntimeException("Error while constructing Action", ex);
        }
    }

    public final void putTask(Class<? extends Task> taskClass) {
        try {
            put(taskClass.getConstructor().newInstance());
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("Task must have default empty constructor to be registered.");
        } catch (Exception ex) {
            throw new RuntimeException("Error while constructing Task", ex);
        }
    }

    /**
     * Stream of all available actions
     *
     * @return
     */
    @ProvidesNames(Action.ACTION_TARGET)
    public Stream<String> listActions() {
        return this.actionMap.keySet().stream();
    }

    /**
     * Stream of all available tasks
     *
     * @return
     */
    @ProvidesNames(Task.TASK_TARGET)
    public Stream<String> listTasks() {
        return this.taskMap.keySet().stream();
    }

    public static class Factory extends PluginFactory {

        @NotNull
        @Override
        public ActionManager build(@NotNull Meta meta) {
            return new ActionManager();
        }

        @NotNull
        @Override
        public Class<? extends Plugin> getType() {
            return ActionManager.class;
        }
    }

}
