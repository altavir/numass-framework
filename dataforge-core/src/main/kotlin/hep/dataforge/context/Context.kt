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

import hep.dataforge.Named
import hep.dataforge.context.Plugin.Companion.PLUGIN_TARGET
import hep.dataforge.data.binary.Binary
import hep.dataforge.data.binary.StreamBinary
import hep.dataforge.io.IOUtils
import hep.dataforge.io.OutputManager
import hep.dataforge.meta.KMetaBuilder
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaID
import hep.dataforge.meta.buildMeta
import hep.dataforge.nullable
import hep.dataforge.optional
import hep.dataforge.providers.Provider
import hep.dataforge.providers.Provides
import hep.dataforge.providers.ProvidesNames
import hep.dataforge.useMeta
import hep.dataforge.values.BooleanValue
import hep.dataforge.values.Value
import hep.dataforge.values.ValueProvider
import hep.dataforge.values.ValueProvider.Companion.VALUE_TARGET
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.stream.Stream
import kotlin.collections.set
import kotlin.coroutines.CoroutineContext
import kotlin.streams.asSequence
import kotlin.streams.asStream

/**
 *
 *
 * The local environment for anything being done in DataForge framework. Contexts are organized into tree structure with [Global] at the top.
 * Each context has a set of named [Value] properties which are taken from parent context in case they are not found in local context.
 * Context implements [ValueProvider] interface and therefore could be uses as a value source for substitutions etc.
 * Context contains [PluginManager] which could be used any number of configurable named plugins.
 * Also Context has its own logger and [OutputManager] to govern all the input and output being made inside the context.
 * @author Alexander Nozik
 */
open class Context(
        final override val name: String,
        val parent: Context? = Global,
        classLoader: ClassLoader? = null,
        private val properties: MutableMap<String, Value> = ConcurrentHashMap()
) : Provider, ValueProvider, Named, AutoCloseable, MetaID, CoroutineScope {

    /**
     * A class loader for this context. Parent class loader is used by default
     */
    open val classLoader: ClassLoader = classLoader ?: parent?.classLoader ?: Global.classLoader

    /**
     * Plugin manager for this Context
     *
     * @return
     */
    val plugins: PluginManager by lazy { PluginManager(this) }
    var logger: Logger = LoggerFactory.getLogger(name)
    private val lock by lazy { ContextLock(this) }


    /**
     * Decorator for property with name "inheritOutput"
     *
     * If true, then [output] would produce between parent output (if not default) and output of this context
     */
    private var inheritOutput: Boolean
        get() = properties.getOrDefault("inheritOutput", BooleanValue.TRUE).boolean
        set(value) = properties.set("inheritOutput", BooleanValue.ofBoolean(value))

    /**
     * Return IO manager of this context. By default parent IOManager is
     * returned.
     *
     * If [inheritOutput] is true and output is present for this context, then  produce split between parent output an this
     *
     * @return the io
     */
    var output: OutputManager
        get() {
            val thisOutput = plugins[OutputManager::class, false]
            return when {
                parent == null -> thisOutput ?: Global.consoleOutputManager // default console for Global
                thisOutput == null -> parent.output// return parent output manager if not defined for this one
                !inheritOutput -> thisOutput // do not inherit parent output
                else -> {
                    val prentOutput = parent.output
                    if (prentOutput === Global.consoleOutputManager) {
                        thisOutput
                    } else {
                        OutputManager.split(thisOutput, parent.output)
                    }

                }
            }
        }
        set(newOutput) {
            //remove old output
            lock.operate {
                plugins.get<OutputManager>(false)?.let { plugins.remove(it) }
                plugins.load(newOutput)
            }
        }


    /**
     * A property showing that dispatch thread is started in the context
     */
    private var started = false

    /**
     * A dispatch thread executor for current context
     *
     * @return
     */
    val dispatcher: ExecutorService by lazy {
        logger.info("Initializing dispatch thread executor in {}", name)
        Executors.newSingleThreadExecutor { r ->
            Thread(r).apply {
                priority = 8 // slightly higher priority
                isDaemon = true
                name = this@Context.name + "_dispatch"
            }.also { started = true }
        }
    }

    /**
     * Find out if context is locked
     *
     * @return
     */
    val isLocked: Boolean
        get() = lock.isLocked

    open val history: Chronicler
        get() = plugins[Chronicler::class] ?: parent?.history ?: Global.history

    /**
     * {@inheritDoc} namespace does not work
     */
    override fun optValue(path: String): Optional<Value> {
        return (properties[path] ?: parent?.optValue(path).nullable).optional
    }

    /**
     * Add property to context
     *
     * @param name
     * @param value
     */
    fun setValue(name: String, value: Any) {
        lock.operate { properties[name] = Value.of(value) }
    }

    override fun getDefaultTarget(): String {
        return Plugin.PLUGIN_TARGET
    }

    @Provides(Plugin.PLUGIN_TARGET)
    fun getPlugin(pluginName: String): Plugin? {
        return plugins.get(PluginTag.fromString(pluginName))
    }

    @ProvidesNames(Plugin.PLUGIN_TARGET)
    fun listPlugins(): Collection<String> {
        return plugins.map { it.name }
    }

    @ProvidesNames(ValueProvider.VALUE_TARGET)
    fun listValues(): Collection<String> {
        return properties.keys
    }


    fun getProperties(): Map<String, Value> {
        return Collections.unmodifiableMap(properties)
    }


    inline fun <reified T : Any> get(): T? {
        return get(T::class.java)
    }

    @JvmOverloads
    fun <T : Plugin> load(type: Class<T>, meta: Meta = Meta.empty()): T {
        return plugins.load(type, meta)
    }

    inline fun <reified T : Plugin> load(noinline metaBuilder: KMetaBuilder.() -> Unit = {}): T {
        return plugins.load(metaBuilder)
    }


    /**
     * Opt a plugin extending given class
     *
     * @param type
     * @param <T>
     * @return
     */
    operator fun <T : Any> get(type: Class<T>): T? {
        return plugins
                .stream(true)
                .asSequence().filterIsInstance(type)
                .firstOrNull()
    }

    /**
     * Get existing plugin or load it with default meta
     */
    fun <T : Plugin> getOrLoad(type: Class<T>): T {
        return get(type) ?: load(type)
    }

    private val serviceCache: MutableMap<Class<*>, ServiceLoader<*>> = HashMap()

    /**
     * Get stream of services of given class provided by Java SPI or any other service loading API.
     *
     * @param serviceClass
     * @param <T>
     * @return
     */
    fun <T> serviceStream(serviceClass: Class<T>): Stream<T> {
        synchronized(serviceCache) {
            val loader: ServiceLoader<*> = serviceCache.getOrPut(serviceClass) { ServiceLoader.load(serviceClass, classLoader) }
            return loader.asSequence().filterIsInstance(serviceClass).asStream()
        }
    }

    /**
     * Find specific service provided by java SPI
     */
    fun <T> findService(serviceClass: Class<T>, condition: (T) -> Boolean): T? {
        return serviceStream(serviceClass).filter(condition).findFirst().nullable
    }

    /**
     * Get identity for this context
     *
     * @return
     */
    override fun toMeta(): Meta {
        return buildMeta("context") {
            update(properties)
            plugins.stream(true).forEach { plugin ->
                if (plugin.javaClass.isAnnotationPresent(PluginDef::class.java)) {
                    if (!plugin.javaClass.getAnnotation(PluginDef::class.java).support) {
                        putNode(plugin.toMeta())
                    }

                }
            }
        }
    }

    /**
     * Lock this context by given object
     *
     * @param obj
     */
    fun lock(obj: Any) {
        this.lock.lock(obj)
        parent?.lock(obj)
    }

    /**
     * Unlock the context by given object
     *
     * @param obj
     */
    fun unlock(obj: Any) {
        this.lock.unlock(obj)
        parent?.unlock(obj)
    }


    /**
     * Free up resources associated with this context
     *
     * @throws Exception
     */
    @Throws(Exception::class)
    override fun close() {
        logger.info("Closing context: $name")
        //detach all plugins
        plugins.close()

        if (started) {
            coroutineContext.cancel()
            dispatcher.shutdown()
        }
    }


    /**
     * Return the root directory for this IOManager. By convention, Context
     * should not have access outside root directory to prevent System damage.
     *
     * @return a [java.io.File] object.
     */
    val rootDir: Path by lazy {
        properties[ROOT_DIRECTORY_CONTEXT_KEY]
                ?.let { value -> Paths.get(value.string).also { Files.createDirectories(it) } }
                ?: parent?.rootDir
                ?: File(System.getProperty("user.home")).toPath()
    }

    /**
     * The working directory for output and temporary files. Is always inside root directory
     *
     * @return
     */
    val workDir: Path by lazy {
        val workDirProperty = properties[WORK_DIRECTORY_CONTEXT_KEY]
        when {
            workDirProperty != null -> rootDir.resolve(workDirProperty.string).also { Files.createDirectories(it) }
            else -> rootDir.resolve(".dataforge").also { Files.createDirectories(it) }
        }
    }

    /**
     * Get the default directory for file data. By default uses context root directory
     * @return
     */
    val dataDir: Path by lazy {
        properties[DATA_DIRECTORY_CONTEXT_KEY]?.let { IOUtils.resolvePath(it.string) } ?: rootDir
    }

    /**
     * The directory for temporary files. This directory could be cleaned up any
     * moment. Is always inside root directory.
     *
     * @return
     */
    val tmpDir: Path by lazy {
        properties[TEMP_DIRECTORY_CONTEXT_KEY]
                ?.let { value -> rootDir.resolve(value.string).also { Files.createDirectories(it) } }
                ?: rootDir.resolve(".dataforge/.temp").also { Files.createDirectories(it) }
    }


    fun getDataFile(path: String): FileReference {
        return FileReference.openDataFile(this, path)
    }

    /**
     * Get a file where `path` is relative to root directory or absolute.
     * @param path a [java.lang.String] object.
     * @return a [java.io.File] object.
     */
    fun getFile(path: String): FileReference {
        return FileReference.openFile(this, path)
    }

    /**
     * Get the context based classpath resource
     */
    fun getResource(name: String): Binary? {
        val resource = classLoader.getResource(name)
        return resource?.let { StreamBinary { it.openStream() } }
    }

    /**
     * For anything but values and plugins, list all elements with given target, provided by plugins
     */
    override fun <T : Any?> provideAll(target: String, type: Class<T>): Stream<T> {
        return if (target == PLUGIN_TARGET || target == VALUE_TARGET) {
            super.provideAll(target, type)
        } else {
            plugins.stream(true).flatMap { it.provideAll(target, type) }
        }
    }


    open val executors: ExecutorPlugin
        get() = plugins[ExecutorPlugin::class] ?: parent?.executors ?: Global.executors

    override val coroutineContext: CoroutineContext
        get() = this.executors.coroutineContext

    companion object {

        const val ROOT_DIRECTORY_CONTEXT_KEY = "rootDir"
        const val WORK_DIRECTORY_CONTEXT_KEY = "workDir"
        const val DATA_DIRECTORY_CONTEXT_KEY = "dataDir"
        const val TEMP_DIRECTORY_CONTEXT_KEY = "tempDir"

        /**
         * Build a new context based on given meta
         *
         * @param name
         * @param parent
         * @param meta
         * @return
         */
        @JvmOverloads
        @JvmStatic
        fun build(name: String, parent: Context = Global, meta: Meta = Meta.empty()): Context {
            val builder = ContextBuilder(name, parent)

            meta.useMeta("properties") { builder.properties(it) }

            meta.optString("rootDir").ifPresent { builder.setRootDir(it) }

            meta.optValue("classpath").ifPresent { value -> value.list.stream().map<String> { it.string }.forEach { builder.classPath(it) } }

            meta.getMetaList("plugin").forEach { builder.plugin(it) }

            return builder.build()
        }
    }

}
