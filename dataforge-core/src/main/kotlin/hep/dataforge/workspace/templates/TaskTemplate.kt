package hep.dataforge.workspace.templates

import hep.dataforge.Named
import hep.dataforge.utils.ContextMetaFactory
import hep.dataforge.workspace.tasks.Task

/**
 * A factory to create a task from meta
 */
interface TaskTemplate : ContextMetaFactory<Task<*>>, Named
