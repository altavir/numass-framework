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
public class MeasurementTimeoutException extends MeasurementException {

    /**
     * Creates a new instance of <code>MeasurementTimeoutException</code>
     * without detail message.
     */
    public MeasurementTimeoutException() {
    }

    /**
     * Constructs an instance of <code>MeasurementTimeoutException</code> with
     * the specified detail message.
     *
     * @param msg the detail message.
     */
    public MeasurementTimeoutException(String msg) {
        super(msg);
    }
}
