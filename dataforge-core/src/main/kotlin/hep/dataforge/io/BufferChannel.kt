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

package hep.dataforge.io

import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel


/**
 * A channel that reads o writes inside large buffer
 */
class BufferChannel(val buffer: ByteBuffer) : SeekableByteChannel {
    override fun isOpen(): Boolean {
        return true
    }

    override fun position(): Long {
        return buffer.position().toLong()
    }

    override fun position(newPosition: Long): SeekableByteChannel {
        buffer.position(newPosition.toInt())
        return this
    }

    override fun write(src: ByteBuffer): Int {
        buffer.put(src)
        return src.remaining()
    }

    override fun size(): Long {
        return buffer.limit().toLong()
    }

    override fun close() {
        //do nothing
    }

    override fun truncate(size: Long): SeekableByteChannel {
        if(size< buffer.limit()){
            buffer.limit(size.toInt())
        }
        return this
    }

    override fun read(dst: ByteBuffer): Int {
        val array = ByteArray(dst.remaining())
        buffer.get(array)
        dst.put(array)
        return array.size;
    }

}