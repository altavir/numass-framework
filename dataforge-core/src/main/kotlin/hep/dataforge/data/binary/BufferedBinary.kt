/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.data.binary

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel


class BufferedBinary(override val buffer: ByteBuffer) : Binary {

    override val stream: InputStream
        get() = ByteArrayInputStream(buffer.array())

    override val channel: ReadableByteChannel
        get() = Channels.newChannel(stream)

    constructor(buffer: ByteArray): this(ByteBuffer.wrap(buffer))

    override val size: Long
        get() {
            return buffer.limit().toLong()
        }

}
