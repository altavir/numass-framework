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
import java.net.URI
import java.util.*
import javax.cache.CacheManager
import javax.cache.configuration.OptionalFeature
import javax.cache.spi.CachingProvider

/**
 * Created by darksnake on 08-Feb-17.
 */
class DefaultCachingProvider(override val context: Context) : CachingProvider, ContextAware {

    override fun getCacheManager(uri: URI, classLoader: ClassLoader, properties: Properties): CacheManager? {
        return null
    }

    override fun getDefaultClassLoader(): ClassLoader {
        return context.classLoader
    }

    override fun getDefaultURI(): URI? {
        return null
    }

    override fun getDefaultProperties(): Properties? {
        return null
    }

    override fun getCacheManager(uri: URI, classLoader: ClassLoader): CacheManager? {
        return null
    }

    override fun getCacheManager(): CacheManager? {
        return null
    }

    override fun close() {

    }

    override fun close(classLoader: ClassLoader) {

    }

    override fun close(uri: URI, classLoader: ClassLoader) {

    }

    override fun isSupported(optionalFeature: OptionalFeature): Boolean {
        return false
    }
}
