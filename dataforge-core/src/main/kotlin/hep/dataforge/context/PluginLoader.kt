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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.context

import hep.dataforge.meta.Meta
import hep.dataforge.nullable
import hep.dataforge.utils.MetaFactory
import java.util.*
import java.util.stream.Stream
import java.util.stream.StreamSupport
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf


/**
 * Created by darksnake on 08-Sep-16.
 */
abstract class PluginFactory() : MetaFactory<Plugin> {

    abstract val type: Class<out Plugin>

    val tag: PluginTag
        get() = PluginTag.resolve(type)

    override fun build(meta: Meta): Plugin {
        try {
            val constructor = type.getConstructor(Meta::class.java)
            return constructor.newInstance(meta) as Plugin
        } catch (e: Exception) {
            throw RuntimeException("Failed to build plugin $tag using default constructor")
        }

    }

    companion object {
        fun fromClass(type: Class<out Plugin>): PluginFactory {
            return object : PluginFactory() {
                override val type: Class<out Plugin> = type
            }
        }
    }
}

/**
 * A resolution strategy for plugins
 *
 * @author Alexander Nozik
 */
interface PluginLoader {

    /**
     * Load the most suitable plugin of all provided by its tag
     *
     * @param tag
     * @return
     */
    fun opt(tag: PluginTag, meta: Meta): Plugin?

    fun <T : Plugin> opt(type: KClass<T>, meta: Meta): T?


    operator fun get(tag: PluginTag, meta: Meta): Plugin {
        return opt(tag, meta) ?: throw RuntimeException("No plugin matching $tag found")
    }

    operator fun <T : Plugin> get(type: KClass<T>, meta: Meta): T {
        return opt(type, meta) ?: throw RuntimeException("No plugin of type $type found")
    }

    /**
     * List tags provided by this repository
     *
     * @return
     */
    fun listTags(): List<PluginTag>


}


/**
 * Created by darksnake on 10-Apr-17.
 */
abstract class AbstractPluginLoader : PluginLoader {
    override fun opt(tag: PluginTag, meta: Meta): Plugin? {
        return factories()
                .filter { factory -> tag.matches(factory.tag) }
                .sorted { p1, p2 -> this.compare(p1, p2) }
                .findFirst().map { it -> it.build(meta) }.nullable
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Plugin> opt(type: KClass<T>, meta: Meta): T {
        val factory = factories()
                .filter { factory -> factory.type.kotlin.isSubclassOf(type) }
                .sorted { p1, p2 -> this.compare(p1, p2) }
                .findFirst().orElseGet { PluginFactory.fromClass(type.java) }
        return factory.build(meta) as T
        //.map { it -> it.build(meta) }.nullable as T?
    }


    protected fun compare(p1: PluginFactory, p2: PluginFactory): Int {
        return Integer.compare(p1.tag.getInt("priority", 0), p2.tag.getInt("priority", 0))
    }

    override fun listTags(): List<PluginTag> {
        return factories().map { it.tag }.toList()
    }

    protected abstract fun factories(): Stream<PluginFactory>
}

/**
 * The plugin resolver that searches classpath for Plugin services and loads the
 * best one
 *
 * @author Alexander Nozik
 */
class ClassPathPluginLoader(context: Context) : AbstractPluginLoader() {

    private val loader: ServiceLoader<PluginFactory>

    init {
        val cl = context.classLoader
        loader = ServiceLoader.load(PluginFactory::class.java, cl)
    }

    override fun factories(): Stream<PluginFactory> {
        return StreamSupport.stream(loader.spliterator(), false)
    }
}