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

package inr.numass.data

import hep.dataforge.meta.Meta
import hep.dataforge.storage.files.MutableFileEnvelope
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class NumassFileEnvelope(path: Path) : MutableFileEnvelope(path) {

    private val tag by lazy { Files.newByteChannel(path, StandardOpenOption.READ).use { NumassEnvelopeType.LegacyTag().read(it) } }

    override val dataOffset: Long by lazy { (tag.length + tag.metaSize).toLong() }

    override var dataLength: Int
        get() = tag.dataSize
        set(value) {
            if (value > Int.MAX_VALUE) {
                throw RuntimeException("Too large data block")
            }
            tag.dataSize = value
            if (channel.write(tag.toBytes(), 0L) < tag.length) {
                throw error("Tag is not overwritten.")
            }
        }


    override val meta: Meta by lazy {
        val buffer = ByteBuffer.allocate(tag.metaSize).also {
            channel.read(it, tag.length.toLong())
        }
        tag.metaType.reader.readBuffer(buffer)
    }
}

