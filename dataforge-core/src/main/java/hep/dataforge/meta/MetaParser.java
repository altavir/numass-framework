/* 
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hep.dataforge.meta;

import hep.dataforge.context.ContextAware;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.ParseException;

/**
 * An interface which allows conversion from Meta to string and vise versa
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public interface MetaParser extends ContextAware {
    
    /**
     * Convert Annotation to String
     *
     * @param source a T object.
     * @return a {@link java.lang.String} object.
     */
    String toString(Meta source);

    /**
     * Convert String representation to Annotation object
     *
     * @param string a {@link java.lang.String} object.
     * @return a T object.
     * @throws java.text.ParseException if any.
     */
    Meta fromString(String string) throws ParseException;

    /**
     * read {@code length} bytes from InputStream and interpret it as
     * Annotation. If {@code length < 0} then parse input stream until end of
     * annotation is found.
     *
     * @param stream a {@link java.io.InputStream} object.
     * @param length a long.
     * @param encoding a {@link java.nio.charset.Charset} object.
     * @throws java.io.IOException if any.
     * @throws java.text.ParseException if any.
     * @return a {@link hep.dataforge.meta.Meta} object.
     */
    Meta fromStream(InputStream stream, long length, Charset encoding) throws IOException, ParseException;
}
