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
 *
 * @author Alexander Nozik
 */
public class PortException extends ControlException {

    /**
     * Creates a new instance of <code>PortException</code> without detail
     * message.
     */
    public PortException() {
    }

    /**
     * Constructs an instance of <code>PortException</code> with the specified
     * detail message.
     *
     * @param msg the detail message.
     */
    public PortException(String msg) {
        super(msg);
    }

    public PortException(String message, Throwable cause) {
        super(message, cause);
    }

    public PortException(Throwable cause) {
        super(cause);
    }

}
