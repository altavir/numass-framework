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
 * This exception is used when some parameters or functions are not defined by
 * user.
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class NotDefinedException extends IllegalStateException {

    /**
     * Creates a new instance of
     * <code>NotDefinedException</code> without detail message.
     */
    public NotDefinedException() {
    }

    /**
     * Constructs an instance of
     * <code>NotDefinedException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public NotDefinedException(String msg) {
        super(msg);
    }
}
