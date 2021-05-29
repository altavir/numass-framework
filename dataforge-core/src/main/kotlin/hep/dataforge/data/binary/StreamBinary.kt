/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.data.binary

import java.io.IOException
import java.io.InputStream
import java.io.ObjectStreamException
import java.io.WriteAbortedException
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel

class StreamBinary(private val sup: () -> InputStream) : Binary {
    //TODO limit inputStream size
    override val stream: InputStream by lazy(sup)

    override val channel: ReadableByteChannel by lazy {
        Channels.newChannel(stream)
    }

    override val size: Long
        get() = -1

    @Throws(ObjectStreamException::class)
    private fun writeReplace(): Any {
        try {
            return BufferedBinary(buffer.array())
        } catch (e: IOException) {
            throw WriteAbortedException("Failed to get byte buffer", e)
        }

    }

}
