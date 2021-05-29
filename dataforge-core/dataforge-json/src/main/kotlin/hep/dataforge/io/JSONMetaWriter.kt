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
import hep.dataforge.meta.Meta
import hep.dataforge.values.Value
import hep.dataforge.values.ValueType
import java.io.OutputStream

/**
 * A converter from Meta object to JSON character stream
 *
 * @author Alexander Nozik
 */
object JSONMetaWriter : MetaStreamWriter {

    override fun write(stream: OutputStream, meta: Meta) {
        val json = meta.toJson()
        val string = Jsoner.prettyPrint(Jsoner.serialize(json))
        stream.write(string.toByteArray(Charsets.UTF_8))
        stream.flush()
    }

    private fun Value.toJson(): Any {
        return if (list.size == 1) {
            when (type) {
                ValueType.NUMBER -> number
                ValueType.BOOLEAN -> boolean
                else -> string
            }
        } else {
            JsonArray().apply {
                list.forEach { add(it.toJson()) }
            }
        }
    }

    private fun Meta.toJson(): JsonObject {
        val builder = JsonObject()
        nodeNames.forEach {
            val nodes = getMetaList(it)
            if (nodes.size == 1) {
                builder[it] = nodes[0].toJson()
            } else {
                val array = JsonArray()
                nodes.forEach { array.add(it.toJson()) }
                builder[it] = array
            }
        }

        valueNames.forEach {
            builder[it] = getValue(it).toJson()
        }

        return builder
    }
}


//private class JSONWriter : StringWriter() {
//
//    private var indentlevel = 0
//
//    override fun write(c: Int) {
//        val ch = c.toChar()
//        if (ch == '[' || ch == '{') {
//            super.write(c)
//            super.write("\n")
//            indentlevel++
//            writeIndentation()
//        } else if (ch == ',') {
//            super.write(c)
//            super.write("\n")
//            writeIndentation()
//        } else if (ch == ']' || ch == '}') {
//            super.write("\n")
//            indentlevel--
//            writeIndentation()
//            super.write(c)
//        } else if (ch == ':') {
//            super.write(c)
//            super.write(spaceaftercolon)
//        } else {
//            super.write(c)
//        }
//
//    }
//
//    private fun writeIndentation() {
//        for (i in 0 until indentlevel) {
//            super.write(indentstring)
//        }
//    }
//
//    companion object {
//        internal val indentstring = "  " //define as you wish
//        internal val spaceaftercolon = " " //use "" if you don't want space after colon
//    }
//}


