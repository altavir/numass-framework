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

import hep.dataforge.context.Context
import hep.dataforge.context.ContextAware
import hep.dataforge.context.Global
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaHolder
import java.net.URI
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.cache.CacheManager
import javax.cache.Caching
import javax.cache.configuration.Configuration
import javax.cache.spi.CachingProvider

/**
 * Created by darksnake on 08-Feb-17.
 */
class DefaultCacheManager(override val context: Context, cfg: Meta) : MetaHolder(cfg), CacheManager, ContextAware {
    private var map: MutableMap<String, DefaultCache<*, *>> = ConcurrentHashMap();

    val rootCacheDir: Path
        get() = context.tmpDir.resolve("cache")

    override fun getCachingProvider(): CachingProvider {
        return DefaultCachingProvider(context)
    }

    override fun getURI(): URI {
        return rootCacheDir.toUri()
    }

    override fun getClassLoader(): ClassLoader {
        return Caching.getDefaultClassLoader()
    }

    override fun getProperties(): Properties {
        return Properties()
    }


    @Throws(IllegalArgumentException::class)
    override fun <K, V, C : Configuration<K, V>> createCache(cacheName: String, configuration: C): DefaultCache<K, V> {
        return DefaultCache(cacheName, this, configuration.keyType, configuration.valueType)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <K, V> getCache(cacheName: String, keyType: Class<K>, valueType: Class<V>): DefaultCache<K, V> {
        return map.getOrPut(cacheName) { DefaultCache(cacheName, this, keyType, valueType) } as DefaultCache<K, V>
    }

    @Suppress("UNCHECKED_CAST")
    override fun <K, V> getCache(cacheName: String): DefaultCache<K, V> {
        return map.getOrPut(cacheName) { DefaultCache(cacheName, this, Any::class.java, Any::class.java) } as DefaultCache<K, V>
    }

    override fun getCacheNames(): Iterable<String> {
        return map.keys
    }

    override fun destroyCache(cacheName: String) {
        val cache = map[cacheName]
        if (cache != null) {
            cache.clear()
            cache.close()
            map.remove(cacheName)
        }
    }

    override fun enableManagement(cacheName: String, enabled: Boolean) {
        //do nothing
    }

    override fun enableStatistics(cacheName: String, enabled: Boolean) {
        //do nothing
    }

    override fun close() {
        map.values.forEach { it.close() }
        map.clear()
    }

    override fun isClosed(): Boolean {
        return map.isEmpty()
    }

    override fun <T> unwrap(clazz: Class<T>): T {
        return if (clazz == DefaultCacheManager::class.java) {
            @Suppress("UNCHECKED_CAST")
            DefaultCacheManager(Global, Meta.empty()) as T
        } else {
            throw IllegalArgumentException("Wrong wrapped class")
        }
    }


}
