package inr.numass.data.legacy;

import hep.dataforge.io.envelopes.EnvelopeTag;
import hep.dataforge.storage.filestorage.FileEnvelope;
import inr.numass.NumassEnvelopeType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.READ;

public class NumassFileEnvelope extends FileEnvelope {

    public static byte[] LEGACY_START_SEQUENCE = {'#','!'};
    public static byte[] LEGACY_END_SEQUENCE = {'!','#','\r','\n'};

    public static FileEnvelope open(Path path, boolean readOnly) {
//        if (!Files.exists(path)) {
//            throw new RuntimeException("File envelope does not exist");
//        }

        try (FileChannel channel = FileChannel.open(path,READ)) {
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, 2);
            if (buffer.compareTo(ByteBuffer.wrap(LEGACY_START_SEQUENCE)) == 0) {
                return new NumassFileEnvelope(path, readOnly);
            } else {
                return FileEnvelope.Companion.open(path, readOnly);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to open file envelope", e);
        }
    }

    private NumassFileEnvelope(Path path, boolean readOnly) {
        super(path, readOnly);
    }

    @Override
    protected EnvelopeTag buildTag() {
        return new NumassEnvelopeType.LegacyTag();
    }
}
