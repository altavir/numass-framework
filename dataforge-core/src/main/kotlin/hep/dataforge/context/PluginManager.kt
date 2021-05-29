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
package hep.dataforge.context

import hep.dataforge.exceptions.ContextLockException
import hep.dataforge.meta.KMetaBuilder
import hep.dataforge.meta.Meta
import hep.dataforge.meta.buildMeta
import java.util.*
import java.util.stream.Stream
import kotlin.reflect.KClass

/**
 * The manager for plugin system. Should monitor plugin dependencies and locks.
 *
 * @property context A context for this plugin manager
 * @author Alexander Nozik
 */
class PluginManager(override val context: Context) : ContextAware, AutoCloseable, Iterable<Plugin> {

    /**
     * A set of loaded plugins
     */
    private val plugins = HashSet<Plugin>()

    /**
     * A class path resolver
     */
    var pluginLoader: PluginLoader = ClassPathPluginLoader(context)

    private val parent: PluginManager? = context.parent?.plugins


    fun stream(recursive: Boolean): Stream<Plugin> {
        return if (recursive && parent != null) {
            Stream.concat(plugins.stream(), parent.stream(true))
        } else {
            plugins.stream()
        }
    }

    /**
     * Get for existing plugin
     */
    fun get(recursive: Boolean = true, predicate: (Plugin) -> Boolean): Plugin? {
        return plugins.find(predicate) ?: if (recursive && parent != null) {
            parent.get(true, predicate)
        } else {
            null
        }
    }

    /**
     * Find a loaded plugin via its tag
     *
     * @param tag
     * @return
     */
    operator fun get(tag: PluginTag, recursive: Boolean = true): Plugin? {
        return get(recursive) { tag.matches(it.tag) }
    }

    /**
     * Find a loaded plugin via its class
     *
     * @param tag
     * @param type
     * @param <T>
     * @return
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T : Plugin> get(type: KClass<T>, recursive: Boolean = true): T? {
        return get(recursive) { type.isInstance(it) } as T?
    }

    inline fun <reified T : Plugin> get(recursive: Boolean = true): T? {
        return get(T::class, recursive)
    }

    /**
     * Load given plugin into this manager and return loaded instance.
     * Throw error if plugin of the same class already exists in manager
     *
     * @param plugin
     * @return
     */
    fun <T : Plugin> load(plugin: T): T {
        if (context.isLocked) {
            throw ContextLockException()
        }
        val existing = get(plugin::class, false)
        if ( existing == null) {
            loadDependencies(plugin)

            logger.info("Loading plugin {} into {}", plugin.name, context.name)
            plugin.attach(context)
            plugins.add(plugin)
            return plugin
        } else if(existing.meta == plugin.meta){
            return existing
        } else{
            throw  RuntimeException("Plugin of type ${plugin::class} already exists in ${context.name}")
        }
    }

    fun loadDependencies(plugin: Plugin){
        for (tag in plugin.dependsOn()) {
            load(tag)
        }
    }

    fun remove(plugin: Plugin){
        if (context.isLocked) {
            throw ContextLockException()
        }
        if (plugins.contains(plugin)){
            logger.info("Removing plugin {} from {}", plugin.name, context.name)
            plugin.detach()
            plugins.remove(plugin)
        }
    }

    /**
     * Get plugin instance via plugin reolver and load it.
     *
     * @param tag
     * @return
     */
    @JvmOverloads
    fun load(tag: PluginTag, meta: Meta = Meta.empty()): Plugin {
        val loaded = get(tag, false)
        return when {
            loaded == null -> load(pluginLoader[tag, meta])
            loaded.meta == meta -> loaded // if meta is the same, return existing plugin
            else -> throw RuntimeException("Can't load plugin with tag $tag. Plugin with this tag and different configuration already exists in context.")
        }
    }

    /**
     * Load plugin by its class and meta. Ignore if plugin with this meta is already loaded.
     */
    fun <T : Plugin> load(type: KClass<T>, meta: Meta = Meta.empty()): T {
        val loaded = get(type, false)
        return when {
            loaded == null -> load(pluginLoader[type, meta])
            loaded.meta.equalsIgnoreName(meta) -> loaded // if meta is the same, return existing plugin
            else -> throw RuntimeException("Can't load plugin with type $type. Plugin with this type and different configuration already exists in context.")
        }
    }

    inline fun <reified T : Plugin> load(noinline metaBuilder: KMetaBuilder.() -> Unit = {}): T {
        return load(T::class, buildMeta("plugin", metaBuilder))
    }

    @JvmOverloads
    fun <T : Plugin> load(type: Class<T>, meta: Meta = Meta.empty()): T {
        return load(type.kotlin, meta)
    }

    @JvmOverloads
    fun load(name: String, meta: Meta = Meta.empty()): Plugin {
        return load(PluginTag.fromString(name), meta)
    }

    /**
     * Get existing plugin or load it with default meta
     */
    fun <T : Plugin> getOrLoad(type: KClass<T>): T {
        return get(type) ?: load(type)
    }

    @Throws(Exception::class)
    override fun close() {
        this.plugins.forEach { it.detach() }
    }

    override fun iterator(): Iterator<Plugin> = plugins.iterator()

}
