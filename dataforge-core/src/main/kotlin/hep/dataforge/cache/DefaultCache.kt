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

package hep.dataforge.cache

import hep.dataforge.Named
import hep.dataforge.context.Context
import hep.dataforge.context.ContextAware
import hep.dataforge.io.envelopes.*
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaHolder
import hep.dataforge.meta.MetaMorph
import hep.dataforge.nullable
import hep.dataforge.utils.Misc
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.WRITE
import java.util.*
import javax.cache.Cache
import javax.cache.configuration.CacheEntryListenerConfiguration
import javax.cache.configuration.Configuration
import javax.cache.integration.CompletionListener
import javax.cache.processor.EntryProcessor
import javax.cache.processor.EntryProcessorException
import javax.cache.processor.EntryProcessorResult

/**
 * Default implementation for jCache caching
 * Created by darksnake on 10-Feb-17.
 */
class DefaultCache<K, V>(
        private val name: String,
        private val manager: DefaultCacheManager,
        private val keyType: Class<K>,
        private val valueType: Class<V>) : MetaHolder(manager.meta), Cache<K, V>, ContextAware {

    private val softCache: MutableMap<K, V> by lazy {
        Misc.getLRUCache<K, V>(meta.getInt("softCache.size", 500))
    }

    private val hardCache = HashMap<Meta, Path>()
    private val cacheDir: Path = manager.rootCacheDir.resolve(name)
        get() {
            Files.createDirectories(field)
            return field
        }

    init {
        scanDirectory()
    }

    //    private Envelope read(Path file) {
    //        return reader.read(file);
    //    }

    @Synchronized
    private fun scanDirectory() {
        if (hardCacheEnabled()) {
            hardCache.clear()
            try {
                Files.list(cacheDir).filter { it -> it.endsWith("df") }.forEach { file ->
                    try {
                        val envelope = reader.read(file)
                        hardCache[envelope.meta] = file
                    } catch (e: Exception) {
                        logger.error("Failed to read cache file {}. Deleting corrupted file.", file.toString())
                        file.toFile().delete()
                    }
                }
            } catch (e: IOException) {
                throw RuntimeException("Can't list contents of" + cacheDir.toString())
            }

        }
    }

    private fun getID(key: K): Meta {
        return when (key) {
            is Meta -> key
            is MetaMorph -> key.toMeta()
            else -> throw RuntimeException("Can't convert the cache key to meta")
        }
    }

    override fun get(key: K): V? {
        val id: Meta = getID(key)

        return softCache[key] ?: getFromHardCache(id).map<V> { cacheFile ->
            try {
                ObjectInputStream(reader.read(cacheFile).data.stream).use { ois ->
                    (valueType.cast(ois.readObject())).also {
                        softCache[key] = it
                    }
                }
            } catch (ex: Exception) {
                logger.error("Failed to read cached object with id '{}' from file with message: {}. Deleting corrupted file.", id.toString(), ex.message)
                hardCache.remove(id)
                cacheFile.toFile().delete()
                null
            }
        }.nullable
    }


    override fun getAll(keys: Set<K>): Map<K, V>? {
        return null
    }

    override fun containsKey(key: K): Boolean {
        val id: Meta = getID(key)
        return softCache.containsKey(key) || getFromHardCache(id).isPresent
    }

    private fun getFromHardCache(id: Meta): Optional<Path> {
        //work around for meta numeric hashcode inequality
        return hardCache.entries.stream().filter { entry -> entry.key == id }.findFirst().map { it.value }
    }

    override fun loadAll(keys: Set<K>, replaceExistingValues: Boolean, completionListener: CompletionListener) {

    }

    private fun hardCacheEnabled(): Boolean {
        return meta.getBoolean("fileCache.enabled", true)
    }

    @Synchronized
    override fun put(key: K, data: V) {
        val id: Meta = getID(key)
        softCache[key] = data
        if (hardCacheEnabled() && data is Serializable) {
            var fileName = data.javaClass.simpleName
            if (data is Named) {
                fileName += "[" + (data as Named).name + "]"
            }
            fileName += Integer.toUnsignedLong(id.hashCode()).toString() + ".df"

            val file = cacheDir.resolve(fileName)

            try {
                Files.newOutputStream(file, WRITE, CREATE).use { fos ->
                    val baos = ByteArrayOutputStream()
                    val oos = ObjectOutputStream(baos)
                    oos.writeObject(data)
                    val builder = EnvelopeBuilder().meta(id).data(baos.toByteArray())
                    baos.close()
                    writer.write(fos, builder.build())
                    hardCache.put(id, file)
                }
            } catch (ex: IOException) {
                logger.error("Failed to write data with id hashcode '{}' to file with message: {}", id.hashCode(), ex.message)
            }

        }
    }

    override fun getAndPut(key: K, value: V): V {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun putAll(map: Map<out K, V>) {
        map.forEach { id, data -> this.put(id, data) }
    }

    override fun putIfAbsent(key: K, value: V): Boolean {
        throw UnsupportedOperationException()
    }

    override fun remove(key: K): Boolean {
        throw UnsupportedOperationException()
    }

    override fun remove(key: K, oldValue: V): Boolean {
        throw UnsupportedOperationException()
    }

    override fun getAndRemove(key: K): V {
        throw UnsupportedOperationException()
    }

    override fun replace(key: K, oldValue: V, newValue: V): Boolean {
        throw UnsupportedOperationException()
    }

    override fun replace(key: K, value: V): Boolean {
        throw UnsupportedOperationException()
    }

    override fun getAndReplace(key: K, value: V): V {
        throw UnsupportedOperationException()
    }

    override fun removeAll(keys: Set<K>) {
        throw UnsupportedOperationException()
    }

    override fun removeAll() {
        clear()
    }

    override fun clear() {
        //TODO add uninitialized check
        softCache.clear()
        try {
            if (hardCacheEnabled() && Files.exists(cacheDir)) {
                cacheDir.toFile().deleteRecursively()
            }
        } catch (e: IOException) {
            logger.error("Failed to delete cache directory {}", cacheDir, e)
        }

    }

    @Throws(EntryProcessorException::class)
    override fun <T> invoke(key: K, entryProcessor: EntryProcessor<K, V, T>, vararg arguments: Any): T {
        throw UnsupportedOperationException()
    }

    override fun <T> invokeAll(keys: Set<K>, entryProcessor: EntryProcessor<K, V, T>, vararg arguments: Any): Map<K, EntryProcessorResult<T>> {
        throw UnsupportedOperationException()
    }

    override fun getName(): String {
        return name
    }

    override fun getCacheManager(): DefaultCacheManager {
        return manager
    }

    override fun close() {

    }

    override fun isClosed(): Boolean {
        return false
    }

    override fun <T> unwrap(clazz: Class<T>): T {
        return clazz.cast(this)
    }

    override fun registerCacheEntryListener(cacheEntryListenerConfiguration: CacheEntryListenerConfiguration<K, V>) {
        throw UnsupportedOperationException()
    }

    override fun deregisterCacheEntryListener(cacheEntryListenerConfiguration: CacheEntryListenerConfiguration<K, V>) {
        throw UnsupportedOperationException()
    }

    override fun iterator(): MutableIterator<Cache.Entry<K, V>> {
        return softCache.entries.stream()
                .map { entry -> DefaultEntry(entry.key) { entry.value } }
                .iterator()
    }


    override fun <C : Configuration<K, V>> getConfiguration(clazz: Class<C>): C {
        return clazz.cast(MetaCacheConfiguration(meta, valueType))
    }

    override val context: Context = cacheManager.context

    private inner class DefaultEntry(private val key: K, private val supplier: () -> V) : Cache.Entry<K, V> {

        override fun getKey(): K {
            return key
        }

        override fun getValue(): V {
            return supplier()
        }

        override fun <T> unwrap(clazz: Class<T>): T {
            return clazz.cast(this)
        }
    }

    companion object {

        private val reader = DefaultEnvelopeReader()
        private val writer = DefaultEnvelopeWriter(DefaultEnvelopeType.INSTANCE, xmlMetaType)
    }
}
