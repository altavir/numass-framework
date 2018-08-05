package inr.numass.data.legacy

import hep.dataforge.meta.Meta
import hep.dataforge.storage.files.FileEnvelope
import inr.numass.NumassEnvelopeType
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class NumassFileEnvelope(path: Path) : FileEnvelope(path) {

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

//
//    override fun buildTag(): EnvelopeTag {
//        return NumassEnvelopeType.LegacyTag()
//    }
//
//    companion object {
//
//        val LEGACY_START_SEQUENCE = byteArrayOf('#'.toByte(), '!'.toByte())
//        val LEGACY_END_SEQUENCE = byteArrayOf('!'.toByte(), '#'.toByte(), '\r'.toByte(), '\n'.toByte())
//
//        fun open(path: Path, readOnly: Boolean): FileEnvelope {
//            //        if (!Files.exists(path)) {
//            //            throw new RuntimeException("File envelope does not exist");
//            //        }
//
//            try {
//                FileChannel.open(path, READ).use { channel ->
//                    val buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, 2)
//                    return if (buffer.compareTo(ByteBuffer.wrap(LEGACY_START_SEQUENCE)) == 0) {
//                        NumassFileEnvelope(path, readOnly)
//                    } else {
//                        FileEnvelope.open(path, readOnly)
//                    }
//                }
//            } catch (e: IOException) {
//                throw RuntimeException("Failed to open file envelope", e)
//            }
//
//        }
//    }

