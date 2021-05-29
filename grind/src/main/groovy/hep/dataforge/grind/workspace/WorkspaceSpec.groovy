/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package hep.dataforge.grind.workspace

import groovy.transform.CompileStatic
import hep.dataforge.context.Context
import hep.dataforge.grind.Grind
import hep.dataforge.grind.GrindMetaBuilder
import hep.dataforge.grind.helpers.MethodDescription
import hep.dataforge.meta.Meta
import hep.dataforge.workspace.BasicWorkspace
import hep.dataforge.workspace.Workspace
import hep.dataforge.workspace.tasks.Task

/**
 * A DSL helper to build workspace
 * @author Alexander Nozik
 */
@CompileStatic
class WorkspaceSpec {
    private Workspace.Builder builder;
//    private final Context context;

    /**
     * Create a new specification for a workspace
     * @param context - the context for specification it is by default used as a parent for resulting workspace
     */
    WorkspaceSpec(Context context) {
        this.builder = new BasicWorkspace.Builder(context);
//        this.context = context
    }

    /**
     * builder context for the workspace using closure
     */
    def context(@DelegatesTo(value = ContextSpec, strategy = Closure.DELEGATE_FIRST) Closure cl) {
        def contextSpec = new ContextSpec(builder.context)
        def code = cl.rehydrate(contextSpec, this, this)
        code.resolveStrategy = Closure.DELEGATE_FIRST
        code()
        builder.setContext(contextSpec.build())
    }

    Workspace.Builder getBuilder() {
        return builder
    }

    /**
     * Set workspace data
     * @param cl
     * @return
     */
    @MethodDescription("Load data via closure")
    void data(@DelegatesTo(value = DataNodeSpec, strategy = Closure.DELEGATE_FIRST) Closure cl) {
        builder.data("", DataNodeSpec.buildNode(builder.context, cl))
    }

    /**
     * Load a task into the workspace. One can use task libraries like {@link DefaultTaskLib} to define task builders
     * @param task
     * @return
     */
    @MethodDescription("Register a task")
    def task(Task task) {
        builder.task(task)
    }

    /**
     * Load existing task by class
     * @param taskClass
     * @return
     */
    @MethodDescription("Define a task by its class")
    def task(Class<? extends Task> taskClass) {
        builder.task(taskClass.getDeclaredConstructor().newInstance())
    }

    /**
     * Load meta target using grind meta builder
     * @param closure
     * @return
     */
    @MethodDescription("Create a list of targets")
    def targets(Closure closure) {
        MetaSpec spec = new MetaSpec()
        def code = closure.rehydrate(spec, this, this)
        code.resolveStrategy = Closure.DELEGATE_FIRST
        code()
    }

    private class MetaSpec {
        def methodMissing(String methodName, Closure cl) {
            WorkspaceSpec.this.builder.target(Grind.buildMeta(methodName, cl))
        }
    }

    @MethodDescription("Create new meta using Grind builder")
    def target(String name, @DelegatesTo(GrindMetaBuilder) Closure closure = null) {
        this.builder.target(Grind.buildMeta(name, [:], closure))
    }

    def target(String name, Map parameters, @DelegatesTo(GrindMetaBuilder) Closure closure = null) {
        this.builder.target(Grind.buildMeta(name, parameters, closure))
    }

    @MethodDescription("Assign target as meta")
    def target(Meta meta) {
        this.builder.target(meta)
    }
}

