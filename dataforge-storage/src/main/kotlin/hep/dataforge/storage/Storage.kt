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

package hep.dataforge.storage


import hep.dataforge.Named
import hep.dataforge.Type
import hep.dataforge.connections.AutoConnectible
import hep.dataforge.connections.Connection
import hep.dataforge.connections.RoleDef
import hep.dataforge.connections.RoleDefs
import hep.dataforge.context.Context
import hep.dataforge.context.ContextAware
import hep.dataforge.events.EventHandler
import hep.dataforge.meta.*
import hep.dataforge.names.Name
import hep.dataforge.providers.Provider
import hep.dataforge.providers.Provides
import hep.dataforge.providers.ProvidesNames
import hep.dataforge.storage.StorageElement.Companion.STORAGE_TARGET
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import java.util.*
import kotlin.reflect.KClass

/**
 * Generic storage element
 */
@RoleDefs(
        RoleDef(name = Connection.EVENT_HANDLER_ROLE, objectType = EventHandler::class, info = "Handle events produced by this storage"),
        RoleDef(name = Connection.LOGGER_ROLE, objectType = Logger::class, unique = true, info = "The logger for this storage")
)
@Type("hep.dataforge.storage")
interface StorageElement : Named, Metoid, Provider, ContextAware, AutoConnectible, AutoCloseable {
    /**
     * Parent of this storage element if present
     */
    val parent: StorageElement?

    /**
     * Full name relative to root storage
     */

    val fullName: Name
        get() = if (parent == null) {
            Name.empty()
        } else {
            parent?.fullName?.plus(name) ?: Name.ofSingle(name)
        }

    override fun close() {
        //DO nothing
    }

    companion object {
        const val STORAGE_TARGET = "storage"
    }
}

/**
 * Storage tree node
 */
interface Storage : StorageElement {

    /**
     * Top level children of this storage
     */
    fun getChildren(): Collection<StorageElement>

    /**
     * Names of direct children for provider
     */
    @get:ProvidesNames(STORAGE_TARGET)
    val childrenNames: Collection<String>
        get() = runBlocking { getChildren().map { it.name } }

    /**
     * Get storage element (name notation for recursive calls). Null if not present
     */
    @Provides(STORAGE_TARGET)

    operator fun get(name: String): StorageElement? {
        return get(Name.of(name))
    }

    /**
     * Resolve storage element by its fully qualified name
     */

    operator fun get(name: Name): StorageElement? {
        return if (name.length == 1) {
            getChildren().find { it.name == name.unescaped }
        } else {
            (get(name.first) as Storage?)?.get(name.cutFirst())
        }
    }


    override fun getDefaultTarget(): String = STORAGE_TARGET
}

/**
 * Mutable version of the storage
 */
interface MutableStorage : Storage {
    /**
     * Create a new element of the storage. If element with this name already exists, checks meta and either does nothing or throws exception.
     */
    fun create(meta: Meta): StorageElement

    /**
     * Resolve type of new element based on meta or return null if element type could not be resolved
     */
    fun resolveType(meta: Meta): StorageElementType?
}

fun MutableStorage.create(metaBuilder: KMetaBuilder.() -> Unit) = create(buildMeta(MetaBuilder.DEFAULT_META_NAME, metaBuilder))

/**
 * Create a child storage. In general it has the same type ase parent storage, but it is not guaranteed
 */
suspend fun MutableStorage.createShelf(shelfName: String, meta: Meta = Meta.empty()): MutableStorage {
    return create{
        "name" to shelfName
        update(meta)
    } as MutableStorage
}

/**
 * Leaf element of the storage tree.
 * @param T - the type of loader entry
 */
@Type("hep.dataforge.storage.loader")
interface Loader<T : Any> : StorageElement, Iterable<T> {
    /**
     * The explicit type of the element
     */
    val type: KClass<T>
}

/**
 * Loader that could be appended after creation. Appending must be thread safe.
 */
interface AppendableLoader<T : Any> : Loader<T> {

    /**
     * Synchronously append loader and return when operation is complete
     */
    suspend fun append(item: T)
}

/**
 * Loader which could be accessed by keys
 */
interface IndexedLoader<K : Comparable<K>, T : Any> : Loader<T> {
    /**
     * List of available loader keys. Duplicate keys are not allowed
     * TODO find "common" replacement or move functionality up
     */
    val keys: NavigableSet<K>

    /**
     * Get loader element by its key. If key is not present, return null
     */
    suspend fun get(key: K): T?
}

/**
 * Mutable version of indexed loader. Set operation must be thread safe
 */
interface MutableIndexedLoader<K : Comparable<K>, T : Any> : IndexedLoader<K, T> {

    suspend fun set(key: K, value: T)
}

/**
 * A factory that produces storage elements
 */
interface StorageElementType : Named {

    /**
     * Produce an element. The method uses optional parent, but element it produces could be different from the one, produced by parent.
     */
    fun create(context: Context, meta: Meta, parent: StorageElement? = null): StorageElement


    fun create(parent: StorageElement, meta: Meta): StorageElement = create(parent.context, meta, parent)
}