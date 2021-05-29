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
package hep.dataforge.storage

import hep.dataforge.connections.Connectible
import hep.dataforge.connections.Connection
import hep.dataforge.connections.ConnectionFactory
import hep.dataforge.context.Context
import hep.dataforge.context.ContextAware
import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import hep.dataforge.meta.Metoid
import hep.dataforge.names.AnonymousNotAlowed
import hep.dataforge.utils.ContextMetaFactory
import kotlinx.coroutines.runBlocking

/**
 * @author Alexander Nozik
 */
@AnonymousNotAlowed
class StorageConnection(storageFactory: () -> MutableStorage) : Connection, ContextAware {
    val storage by lazy(storageFactory)
    override val context by lazy { storage.context }
    private var isOpen = false

    override fun isOpen(): Boolean = isOpen

    @Throws(Exception::class)
    override fun open(obj: Any) {
        storage
        isOpen = true
    }

    @Throws(Exception::class)
    override fun close() {
        if (isOpen) {
            storage.close()
        }
    }

    class Factory : ConnectionFactory {

        override fun getType(): String {
            return "df.storage"
        }

        override fun <T : Connectible> build(obj: T, context: Context, meta: Meta): Connection {
            return if (obj is Metoid) {
                build(context, Laminate((obj as Metoid).meta.getMetaOrEmpty("storage"), meta)
                )
            } else {
                build(context, meta)
            }
        }
    }

    companion object : ContextMetaFactory<StorageConnection> {
        override fun build(context: Context, meta: Meta): StorageConnection {
            val storageManager = context.plugins.load(StorageManager::class.java)
            return StorageConnection {
                runBlocking {
                    storageManager.create(meta) as MutableStorage
                }
            }
        }
    }

}
