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

package hep.dataforge.io.envelopes

import hep.dataforge.io.MetaStreamReader
import hep.dataforge.io.MetaStreamWriter
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaUtils
import java.io.*
import java.util.*


val binaryMetaType = BinaryMetaType()

/**
 * Binary meta type
 * Created by darksnake on 02-Mar-17.
 */
class BinaryMetaType : MetaType {

    override val codes: List<Short> = listOf(0x4249, 10)//BI

    override val name: String = "binary"

    override val fileNameFilter: (String)->Boolean = { str -> str.lowercase(Locale.getDefault()).endsWith(".meta") }


    override val reader: MetaStreamReader = MetaStreamReader { stream, length ->
        val actualStream = if (length > 0) {
            val bytes = ByteArray(length.toInt())
            stream.read(bytes)
            ByteArrayInputStream(bytes)
        } else {
            stream
        }
        val ois = ObjectInputStream(actualStream)
        MetaUtils.readMeta(ois)
    }

    override val writer = object : MetaStreamWriter {

        @Throws(IOException::class)
        override fun write(stream: OutputStream, meta: Meta) {
            MetaUtils.writeMeta(ObjectOutputStream(stream), meta)
            stream.write('\r'.code)
            stream.write('\n'.code)
        }
    }

}
