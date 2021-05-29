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
package hep.dataforge.cache

import hep.dataforge.context.BasicPlugin
import hep.dataforge.context.Plugin
import hep.dataforge.context.PluginDef
import hep.dataforge.context.PluginFactory
import hep.dataforge.data.Data
import hep.dataforge.data.DataNode
import hep.dataforge.data.DataTree
import hep.dataforge.data.NamedData
import hep.dataforge.goals.Goal
import hep.dataforge.goals.GoalListener
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import java.io.Serializable
import java.util.concurrent.CompletableFuture
import java.util.stream.Stream
import javax.cache.Cache
import javax.cache.CacheException
import javax.cache.CacheManager
import javax.cache.Caching

/**
 * @author Alexander Nozik
 */
@PluginDef(name = "cache", group = "hep.dataforge", info = "Data caching plugin")
class CachePlugin(meta: Meta) : BasicPlugin(meta) {

    /**
     * Set cache bypass condition for data
     *
     * @param bypass
     */
    var bypass: (Data<*>) -> Boolean = { _ -> false }

    private val manager: CacheManager by lazy {
        try {
            Caching.getCachingProvider(context.classLoader).cacheManager.also {
                context.logger.info("Loaded cache manager $it")
            }
        } catch (ex: CacheException) {
            context.logger.warn("Cache provider not found. Will use default cache implementation.")
            DefaultCacheManager(context, meta)
        }
    }

    override fun detach() {
        super.detach()
        manager.close()
    }


    fun <V> cache(cacheName: String, data: Data<V>, id: Meta): Data<V> {
        if (bypass(data) || !Serializable::class.java.isAssignableFrom(data.type)) {
            return data
        } else {
            val cache = getCache(cacheName, data.type)
            val cachedGoal = object : Goal<V> {
                private val result = CompletableFuture<V>()

                override fun dependencies(): Stream<Goal<*>> {
                    return if (cache.containsKey(id)) {
                        Stream.empty()
                    } else {
                        Stream.of(data.goal)
                    }
                }

                override fun run() {
                    //TODO add executor
                    synchronized(cache) {
                        when {
                            data.goal.isDone -> data.future.thenAccept { result.complete(it) }
                            cache.containsKey(id) -> {
                                logger.info("Cached result found. Restoring data from cache for id {}", id.hashCode())
                                CompletableFuture.supplyAsync { cache.get(id) }.whenComplete { res, err ->
                                    if (res != null) {
                                        result.complete(res)
                                    } else {
                                        evalData()
                                    }

                                    if (err != null) {
                                        logger.error("Failed to load data from cache", err)
                                    }
                                }
                            }
                            else -> evalData()
                        }
                    }
                }

                private fun evalData() {
                    data.goal.run()
                    (data.goal as Goal<V>).onComplete { res, err ->
                        if (err != null) {
                            result.completeExceptionally(err)
                        } else {
                            result.complete(res)
                            try {
                                cache.put(id, res)
                            } catch (ex: Exception) {
                                context.logger.error("Failed to put result into the cache", ex)
                            }

                        }
                    }
                }

                override fun asCompletableFuture(): CompletableFuture<V> {
                    return result
                }

                override fun isRunning(): Boolean {
                    return !result.isDone
                }

                override fun registerListener(listener: GoalListener<V>) {
                    //do nothing
                }
            }
            return Data(data.type, cachedGoal, data.meta)
        }
    }

    fun <V : Any> cacheNode(cacheName: String, node: DataNode<V>, nodeId: Meta): DataNode<V> {
        val builder = DataTree.edit(node.type).also {
            it.name = node.name
            it.meta = node.meta
            //recursively caching nodes
            node.nodeStream(false).forEach { child ->
                it.add(cacheNode(Name.joinString(cacheName, child.name), child, nodeId))
            }
            //caching direct data children
            node.dataStream(false).forEach { datum ->
                it.putData(datum.name, cache(cacheName, datum, nodeId.builder.setValue("dataName", datum.name)))
            }
        }

        return builder.build()
    }

    fun <V : Any> cacheNode(cacheName: String, node: DataNode<V>, idFactory: (NamedData<*>) -> Meta): DataNode<V> {
        val builder = DataTree.edit(node.type).also {cached->
            cached.name = node.name
            cached.meta = node.meta
            //recursively caching everything
            node.dataStream(true).forEach { datum ->
                cached.putData(datum.name, cache(cacheName, datum, idFactory.invoke(datum)))
            }
        }

        return builder.build()
    }

    private fun <V> getCache(name: String, type: Class<V>): Cache<Meta, V> {
        return manager.getCache(name, Meta::class.java, type)
                ?: manager.createCache(name, MetaCacheConfiguration(meta, type))
    }

    //    @Override
    //    protected synchronized void applyConfig(Meta config) {
    //        //reset the manager
    //        if (manager != null) {
    //            manager.close();
    //        }
    //        manager = null;
    //        super.applyConfig(config);
    //    }

    fun invalidate(cacheName: String) {
        manager.destroyCache(cacheName)
    }

    fun invalidate() {
        manager.cacheNames?.forEach { this.invalidate(it) }
    }

    class Factory : PluginFactory() {
        override val type: Class<out Plugin>
            get() = CachePlugin::class.java

        override fun build(meta: Meta): Plugin {
            return CachePlugin(meta)
        }
    }
}
