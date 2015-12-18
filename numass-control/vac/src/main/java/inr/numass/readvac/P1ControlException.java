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
package inr.numass.readvac;

/**
 *
 * @author Darksnake
 */
class P1ControlException extends Exception {

    /**
     * Creates a new instance of <code>p1ControlException</code> without detail
     * message.
     */
    public P1ControlException() {
    }

    /**
     * Constructs an instance of <code>p1ControlException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public P1ControlException(String msg) {
        super(msg);
    }
}
