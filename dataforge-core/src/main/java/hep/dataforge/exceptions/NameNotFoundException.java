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
 * <p>NameNotFoundException class.</p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class NameNotFoundException extends NamingException {

    private String name;

    /**
     * Creates a new instance of
     * <code>NameNotFoundException</code> without detail message.
     */
    public NameNotFoundException() {
        super();
    }

    /**
     * Constructs an instance of
     * <code>NameNotFoundException</code> with the specified detail message.
     *
     * @param name a {@link java.lang.String} object.
     */
    public NameNotFoundException(String name) {
        this.name = name;
    }

    /**
     * <p>Constructor for NameNotFoundException.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param msg  a {@link java.lang.String} object.
     */
    public NameNotFoundException(String name, String msg) {
        super(msg);
        this.name = name;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + buildMessage();
    }

    protected String buildMessage() {
        return " The element with name \"" + getName() + "\" is not found.";
    }

    /**
     * <p>Getter for the field <code>name</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return this.name;
    }
}
