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

import hep.dataforge.description.ValueDef
import hep.dataforge.io.history.Chronicle
import hep.dataforge.io.history.History
import hep.dataforge.io.history.Record
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import hep.dataforge.providers.Provides
import hep.dataforge.values.ValueType
import java.util.*

@ValueDef(
    key = "printHistory",
    type = [ValueType.BOOLEAN],
    def = "false",
    info = "If true, print all incoming records in default context output"
)
@PluginDef(
    name = "chronicler",
    group = "hep.dataforge",
    support = true,
    info = "The general support for history logging"
)
class Chronicler(meta: Meta) : BasicPlugin(meta), History {

    private val recordPusher: (Record) -> Unit = { Global.console.render(it) }

    private val root: Chronicle by lazy {
        Chronicle(
            context.name,
            if (context == Global) {
                null
            } else {
                Global.history
            }
        ).also {
            if (meta.getBoolean("printHistory", false)) {
                it.addListener(recordPusher)
            }
        }
    }

    override fun getChronicle(): Chronicle = root


    private val historyCache = HashMap<String, Chronicle>()

    @Provides(Chronicle.CHRONICLE_TARGET)
    fun optChronicle(logName: String): Optional<Chronicle> {
        return Optional.ofNullable(historyCache[logName])
    }

    /**
     * get or builder current log creating the whole log hierarchy
     *
     * @param reportName
     * @return
     */
    fun getChronicle(reportName: String): Chronicle {
        return historyCache[reportName] ?: run {
            val name = Name.of(reportName)
            val parent: History? = when {
                name.length > 1 -> getChronicle(name.cutLast().toString())
                else -> root
            }
            Chronicle(name.last.toString(), parent).also {
                synchronized(this){
                    historyCache[reportName] = it
                }
            }
        }
    }
}