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
package hep.dataforge.exceptions;

/**
 * <p>ContentException class.</p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class ContentException extends RuntimeException {

    /**
     * Creates a new instance of <code>ContentException</code> without detail
     * message.
     */
    public ContentException() {
    }

    /**
     * Constructs an instance of <code>ContentException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public ContentException(String msg) {
        super(msg);
    }

    /**
     * <p>Constructor for ContentException.</p>
     *
     * @param string a {@link java.lang.String} object.
     * @param thrwbl a {@link java.lang.Throwable} object.
     */
    public ContentException(String string, Throwable thrwbl) {
        super(string, thrwbl);
    }
    
    
}
