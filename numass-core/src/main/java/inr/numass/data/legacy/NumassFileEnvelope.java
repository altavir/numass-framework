package inr.numass.data.legacy;

import hep.dataforge.io.envelopes.EnvelopeTag;
import hep.dataforge.storage.filestorage.FileEnvelope;
import inr.numass.NumassEnvelopeType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static java.nio.file.StandardOpenOption.READ;

public class NumassFileEnvelope extends FileEnvelope {

    public static FileEnvelope open(Path path, boolean readOnly) {
        if (!Files.exists(path)) {
            throw new RuntimeException("File envelope does not exist");
        }
        try (InputStream stream = Files.newInputStream(path, READ)) {
            byte[] bytes = new byte[2];
            stream.read(bytes);
            if (Arrays.equals(bytes, NumassEnvelopeType.Companion.getLEGACY_START_SEQUENCE())) {
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
