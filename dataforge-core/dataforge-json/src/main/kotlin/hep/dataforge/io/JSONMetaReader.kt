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
package hep.dataforge.io

import com.github.cliftonlabs.json_simple.JsonArray
import com.github.cliftonlabs.json_simple.JsonObject
import com.github.cliftonlabs.json_simple.Jsoner
import hep.dataforge.meta.KMetaBuilder
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.buildMeta
import hep.dataforge.values.LateParseValue
import hep.dataforge.values.Value
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.text.ParseException

/**
 * Reader for JSON meta
 *
 * @author Alexander Nozik
 */
object JSONMetaReader : MetaStreamReader {

    @Throws(IOException::class, ParseException::class)
    override fun read(stream: InputStream, length: Long): MetaBuilder {
        return if (length == 0L) {
            MetaBuilder("")
        } else {
            val json = if (length > 0) {
                //Read into intermediate buffer
                val buffer = ByteArray(length.toInt())
                stream.read(buffer)
                Jsoner.deserialize(InputStreamReader(ByteArrayInputStream(buffer), Charsets.UTF_8)) as JsonObject
            } else {
                Jsoner.deserialize(InputStreamReader(stream, Charsets.UTF_8)) as JsonObject
            }
            json.toMeta()
        }
    }

    @Throws(ParseException::class)
    private fun JsonObject.toMeta(): MetaBuilder {
        return buildMeta {
            this@toMeta.forEach { key, value -> appendValue(this, key as String, value) }
        }
    }

    private fun JsonArray.toListValue(): Value {
        val list = this.map {value->
            when (value) {
                is JsonArray -> value.toListValue()
                is Number -> Value.of(value)
                is Boolean -> Value.of(value)
                is String -> LateParseValue(value)
                null -> Value.NULL
                is JsonObject -> throw RuntimeException("Object values inside multidimensional arrays are not allowed")
                else -> throw Error("Unknown token $value in json")
            }
        }
        return Value.of(list)
    }

    private fun appendValue(builder: KMetaBuilder, key: String, value: Any?) {
        when (value) {
            is JsonObject -> builder.attachNode(value.toMeta().rename(key))
            is JsonArray -> {
                value.forEach {
                    if(it is JsonArray){
                        builder.putValue(key, it.toListValue())
                    } else {
                        appendValue(builder, key, it)
                    }
                }
            }
            is Number -> builder.putValue(key, value)
            is Boolean -> builder.putValue(key, value)
            is String -> builder.putValue(key, LateParseValue(value))
        //ignore anything else
        }
    }

}
