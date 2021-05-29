package hep.dataforge.grind.misc

import hep.dataforge.data.binary.Binary
import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.io.envelopes.EnvelopeType
import hep.dataforge.io.envelopes.SimpleEnvelope
import hep.dataforge.meta.Meta
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.stream.Collectors

class ReWrapper {

    def logger = LoggerFactory.getLogger("reWrapper");

    def reWrap(Meta meta) {
        Path path = Paths.get(new URI(meta.getString("path")));
        if (Files.isDirectory(path)) {
            String mask = meta.getString("mask", "*");
            String regex = mask.replace(".", "\\.").replace("?", ".?").replace("*", ".+");
            Files.find(path, Integer.MAX_VALUE, { name, attr -> name.toString().matches(regex) })
        } else {
            reWrapFile(path, meta);
        }
    }

    def reWrapFile(Path path, Meta meta) {
        EnvelopeType readType = inferType(path);
        if (readType) {
            Map readProperties = resolveProperties(meta.getMetaOrEmpty("input.properties"));
            logger.info("Reading envelope from file ${path}")

            //reading file content
            Envelope envelope = Files.newInputStream(path, StandardOpenOption.READ).withCloseable {
                //TODO ensure binary is not lazy?
                readType.getReader(readProperties).read(it);
            }

            Envelope newEnvelope = new SimpleEnvelope(transformMeta(envelope.getMeta(), meta), transformData(envelope.data, meta))

            Map writeProperties = resolveProperties(meta.getMetaOrEmpty("output.properties"))
            EnvelopeType writeType = getOutputType(envelope, meta);
            Path newPath = outputPath(path, meta)

            Files.newOutputStream(newPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).withCloseable {
                writeType.getWriter(writeProperties).write(it, newEnvelope);
            }

            logger.info("Finished writing rewrapped envelope to file ${newPath}")

        }
    }

    private Map resolveProperties(Meta meta) {
        return meta.getNodeNames().collect(Collectors.toList()).collectEntries { String it -> [it: meta.getString(it)] }
    }

    /**
     * Return type of the file envelope and null if file is not an envelope
     * @param path
     * @return
     */
    EnvelopeType inferType(Path path) {
        return EnvelopeType.infer(path).orElse(null);
    }

    EnvelopeType getOutputType(Envelope input, Meta meta) {
        return EnvelopeType.resolve(meta.getString("target", "default"))
    }

    Path outputPath(Path input, Meta meta) {
        Path resultPath = input;
        if (meta.hasValue("prefix")) {
            resultPath = resultPath.resolveSibling(meta.getString("prefix") + resultPath.getFileName())
        }
        return resultPath;
    }

    Meta transformMeta(Meta input, Meta config) {
        return input;
    }

    Binary transformData(Binary input, Meta config) {
        return input
    }
}
