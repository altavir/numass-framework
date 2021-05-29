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
package hep.dataforge.context

import hep.dataforge.Named
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.MetaID
import hep.dataforge.meta.Metoid
import hep.dataforge.providers.Provider

/**
 * The interface to define a Context plugin. A plugin stores all runtime features of a context.
 * The plugin is by default configurable and a Provider (both features could be ignored).
 * The plugin must in most cases have an empty constructor in order to be able to load it from library.
 *
 *
 * The plugin lifecycle is the following:
 *
 *
 * create - configure - attach - detach - destroy
 *
 *
 * Configuration of attached plugin is possible for a context which is not in a runtime mode, but it is not recommended.
 *
 * @author Alexander Nozik
 */
interface Plugin : Named, Metoid, ContextAware, Provider, MetaID {

    /**
     * Get tag for this plugin
     *
     * @return
     */
    val tag: PluginTag

    /**
     * The name of this plugin ignoring version and group
     *
     * @return
     */
    override val name: String
        get() = tag.name

    /**
     * Plugin dependencies which are required to attach this plugin. Plugin
     * dependencies must be initialized and enabled in the Context before this
     * plugin is enabled.
     *
     * @return
     */
    fun dependsOn(): Array<PluginTag>

    /**
     * Start this plugin and attach registration info to the context. This method
     * should be called only via PluginManager to avoid dependency issues.
     *
     * @param context
     */
    fun attach(context: Context)

    /**
     * Stop this plugin and remove registration info from context and other
     * plugins. This method should be called only via PluginManager to avoid
     * dependency issues.
     */
    fun detach()


    override fun toMeta(): Meta {
        return MetaBuilder("plugin")
                .putValue("context", context.name)
                .putValue("type", this.javaClass.name)
                .putNode("tag", tag.toMeta())
                .putNode("meta", meta)
                .build()
    }

    companion object {

        const val PLUGIN_TARGET = "plugin"
    }

}
