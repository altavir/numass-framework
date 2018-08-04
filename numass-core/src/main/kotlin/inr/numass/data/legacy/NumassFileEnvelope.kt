package inr.numass.data.legacy

import hep.dataforge.io.envelopes.EnvelopeTag
import hep.dataforge.storage.files.FileEnvelope
import inr.numass.NumassEnvelopeType
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption.READ

class NumassFileEnvelope private constructor(path: Path, readOnly: Boolean) : FileEnvelope(path, readOnly) {

    override fun buildTag(): EnvelopeTag {
        return NumassEnvelopeType.LegacyTag()
    }

    companion object {

        val LEGACY_START_SEQUENCE = byteArrayOf('#'.toByte(), '!'.toByte())
        val LEGACY_END_SEQUENCE = byteArrayOf('!'.toByte(), '#'.toByte(), '\r'.toByte(), '\n'.toByte())

        fun open(path: Path, readOnly: Boolean): FileEnvelope {
            //        if (!Files.exists(path)) {
            //            throw new RuntimeException("File envelope does not exist");
            //        }

            try {
                FileChannel.open(path, READ).use { channel ->
                    val buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, 2)
                    return if (buffer.compareTo(ByteBuffer.wrap(LEGACY_START_SEQUENCE)) == 0) {
                        NumassFileEnvelope(path, readOnly)
                    } else {
                        FileEnvelope.open(path, readOnly)
                    }
                }
            } catch (e: IOException) {
                throw RuntimeException("Failed to open file envelope", e)
            }

        }
    }
}
