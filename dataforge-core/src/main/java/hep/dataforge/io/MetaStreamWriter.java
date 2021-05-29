/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.io;

import hep.dataforge.meta.Meta;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.Charset;

/**
 * The writer of meta to stream in some text or binary format
 *
 * @author Alexander Nozik
 */
public interface MetaStreamWriter {

    /**
     * write Meta object to the giver OuputStream using given charset (if it is
     * possible)
     *
     * @param stream
     * @param meta   charset is used
     */
    void write(@NotNull OutputStream stream, @NotNull Meta meta) throws IOException;


    default String writeString(Meta meta) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            write(baos, meta);
        } catch (IOException e) {
            throw new Error(e);
        }
        return new String(baos.toByteArray(), Charset.forName("UTF-8"));
    }

    default void writeToFile(File file, Meta meta) throws IOException {
        write(new FileOutputStream(file), meta);
    }
}
