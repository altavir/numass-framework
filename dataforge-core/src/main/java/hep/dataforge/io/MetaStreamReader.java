/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.io;

import hep.dataforge.meta.MetaBuilder;
import kotlin.text.Charsets;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;

import static java.nio.file.StandardOpenOption.READ;

/**
 * The reader of stream containing meta in some text or binary format. By
 * default reader returns meta as-is without substitutions and includes so it
 * does not need context to operate.
 *
 * @author Alexander Nozik
 */
public interface MetaStreamReader {

    /**
     * read {@code length} bytes from InputStream and interpret it as
     * MetaBuilder. If {@code length < 0} then parse input stream until end of
     * annotation is found.
     * <p>
     * The returned builder could be later transformed or
     * </p>
     *
     * @param stream a stream that should be read.
     * @param length a number of bytes from stream that should be read. Any
     *               negative value .
     * @return a {@link hep.dataforge.meta.Meta} object.
     * @throws java.io.IOException      if any.
     * @throws java.text.ParseException if any.
     */
    MetaBuilder read(@NotNull InputStream stream, long length) throws IOException, ParseException;

    default MetaBuilder read(InputStream stream) throws IOException, ParseException {
        if(stream == null){
            throw new RuntimeException("Stream is null");
        }
        return read(stream, -1);
    }

    /**
     * Read the Meta from file. The whole file is considered to be Meta file.
     *
     * @param file
     * @return
     */
    default MetaBuilder readFile(Path file) {
        try (InputStream stream = Files.newInputStream(file, READ)) {
            return read(stream, Files.size(file));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Read Meta from string
     *
     * @param string
     * @return
     * @throws IOException
     * @throws ParseException
     */
    default MetaBuilder readString(String string) throws IOException, ParseException {
        byte[] bytes;
        bytes = string.getBytes(Charsets.UTF_8);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return read(bais, bytes.length);
    }

    default MetaBuilder readBuffer(ByteBuffer buffer) throws IOException, ParseException {
        return read(new ByteArrayInputStream(buffer.array()), buffer.limit());
    }
}
