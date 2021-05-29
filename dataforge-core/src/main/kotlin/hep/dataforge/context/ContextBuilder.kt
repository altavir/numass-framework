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

import hep.dataforge.context.Context.Companion.DATA_DIRECTORY_CONTEXT_KEY
import hep.dataforge.context.Context.Companion.ROOT_DIRECTORY_CONTEXT_KEY
import hep.dataforge.io.OutputManager
import hep.dataforge.meta.*
import hep.dataforge.values.Value
import hep.dataforge.values.asValue
import java.io.IOException
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.function.BiPredicate
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * A builder for context
 */
class ContextBuilder(var name: String, val parent: Context = Global) {

    val properties = HashMap<String, Value>()

    private val classPath = ArrayList<URL>()

    private val plugins = HashSet<Plugin>()

    var output: OutputManager? = null
        set(value) {
            plugins.removeIf{it is OutputManager}
            if (value != null) {
                plugins.add(value)
            }
        }

    var rootDir: String
        get() = properties[ROOT_DIRECTORY_CONTEXT_KEY]?.toString() ?: parent.rootDir.toString()
        set(value) {
            val path = parent.rootDir.resolve(value)
            //Add libraries to classpath
            val libPath = path.resolve("lib")
            if (Files.isDirectory(libPath)) {
                classPath(libPath.toUri())
            }
            properties[ROOT_DIRECTORY_CONTEXT_KEY] = path.toString().asValue()
        }

    var dataDir: String
        get() = properties[DATA_DIRECTORY_CONTEXT_KEY]?.toString()
            ?: parent.getString(DATA_DIRECTORY_CONTEXT_KEY, parent.rootDir.toString())
        set(value) {
            properties[DATA_DIRECTORY_CONTEXT_KEY] = value.asValue()
        }

    fun properties(config: Meta): ContextBuilder {
        if (config.hasMeta("property")) {
            config.getMetaList("property").forEach { propertyNode ->
                properties[propertyNode.getString("key")] = propertyNode.getValue("value")
            }
        } else if (config.name == "properties") {
            MetaUtils.valueStream(config).forEach { pair -> properties[pair.first.toString()] = pair.second }
        }
        return this
    }

    fun properties(action: KMetaBuilder.() -> Unit): ContextBuilder = properties(buildMeta("properties", action))

    fun plugin(plugin: Plugin): ContextBuilder {
        this.plugins.add(plugin)
        return this
    }

    /**
     * Load and configure a plugin. Use parent PluginLoader for resolution
     *
     * @param type
     * @param meta
     * @return
     */
    @JvmOverloads
    fun plugin(type: Class<out Plugin>, meta: Meta = Meta.empty()): ContextBuilder {
        val tag = PluginTag.resolve(type)
        return plugin(parent.plugins.pluginLoader[tag, meta])
    }

    inline fun <reified T : Plugin> plugin(noinline metaBuilder: KMetaBuilder.() -> Unit = {}): ContextBuilder {
        return plugin(T::class.java, buildMeta("plugin", metaBuilder))
    }

    fun plugin(tag: String, meta: Meta): ContextBuilder {
        val pluginTag = PluginTag.fromString(tag)
        return plugin(parent.plugins.pluginLoader[pluginTag, meta])
    }


    @Suppress("UNCHECKED_CAST")
    fun plugin(meta: Meta): ContextBuilder {
        val plMeta = meta.getMetaOrEmpty(MetaBuilder.DEFAULT_META_NAME)
        return when {
            meta.hasValue("name") -> plugin(meta.getString("name"), plMeta)
            meta.hasValue("class") -> {
                val type: Class<out Plugin> = Class.forName(meta.getString("class")) as? Class<out Plugin>
                    ?: throw RuntimeException("Failed to initialize plugin from meta")
                plugin(type, plMeta)
            }
            else -> throw IllegalArgumentException("Malformed plugin definition")
        }
    }

    fun classPath(vararg path: URL): ContextBuilder {
        classPath.addAll(Arrays.asList(*path))
        return this
    }

    fun classPath(path: URI): ContextBuilder {
        try {
            classPath.add(path.toURL())
        } catch (e: MalformedURLException) {
            throw RuntimeException("Malformed classpath")
        }

        return this
    }

    /**
     * Create additional classpath from a list of strings
     *
     * @param pathStr
     * @return
     */
    fun classPath(pathStr: String): ContextBuilder {
        val path = Paths.get(pathStr)
        return when {
            Files.isDirectory(path) -> try {
                Files.find(path, -1, BiPredicate { subPath, _ -> subPath.toString().endsWith(".jar") })
                    .map<URI> { it.toUri() }.forEach { this.classPath(it) }
                this
            } catch (e: IOException) {
                throw RuntimeException("Failed to load library", e)
            }
            Files.exists(path) -> classPath(path.toUri())
            else -> this
        }
    }

    fun classPath(paths: Collection<URL>): ContextBuilder {
        classPath.addAll(paths)
        return this
    }

    /**
     * Create new IO manager for this context if needed (using default input and output of parent) and set its root
     *
     * @param rootDir
     * @return
     */
    fun setRootDir(rootDir: String): ContextBuilder {
        this.rootDir = rootDir
        return this
    }

    fun setDataDir(dataDir: String): ContextBuilder {
        this.rootDir = dataDir
        return this
    }

    fun build(): Context {
        // automatically add lib directory
        val classLoader = if (classPath.isEmpty()) {
            null
        } else {
            URLClassLoader(classPath.toTypedArray(), parent.classLoader)
        }

        return Context(name, parent, classLoader, properties).apply {
            //            this@ContextBuilder.output?.let {
//                plugins.load(it)
//            }
            this@ContextBuilder.plugins.forEach {
                plugins.load(it)
            }

//            //If custom paths are defined, use new plugin to direct to them
//            if (properties.containsKey(ROOT_DIRECTORY_CONTEXT_KEY) || properties.containsKey(DATA_DIRECTORY_CONTEXT_KEY)) {
//                if (pluginManager.get<OutputManager>(false) == null) {
//                    pluginManager.load(SimpleOutputManager(Global.console))
//                }
//            }

        }

    }
}

fun ContextBuilder.plugin(key: String, metaBuilder: KMetaBuilder.() -> Unit = {}){
    parent.plugins.load(key, buildMeta(transform = metaBuilder))
}
