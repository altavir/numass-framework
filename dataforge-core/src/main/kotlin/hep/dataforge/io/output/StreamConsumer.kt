package hep.dataforge.io.output

import hep.dataforge.meta.Meta
import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * A stream wrapping the output object. Used for backward compatibility
 */
class StreamConsumer(val output: Output, val meta: Meta = Meta.empty()) : OutputStream() {
    val buffer = ByteArrayOutputStream()

    override fun write(b: Int) {
        synchronized(buffer) {
            when(b.toChar()){
                '\r' -> {}
                '\n' -> flush()
                else -> buffer.write(b)
            }
        }
    }

    override fun flush() {
        synchronized(buffer) {
            if(buffer.size()>0) {
                output.render(String(buffer.toByteArray(), Charsets.UTF_8), meta)
                buffer.reset()
            }
        }
    }

    override fun close() {
        flush()
    }
}