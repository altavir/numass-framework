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
 * <p>TargetNotProvidedException class.</p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class TargetNotProvidedException extends NameNotFoundException {

    /**
     * Creates a new instance of <code>TargetNotProvided</code> without detail
     * message.
     */
    public TargetNotProvidedException(String target) {
        super(target);
    }

    /**
     * Constructs an instance of <code>TargetNotProvided</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public TargetNotProvidedException(String msg, String target) {
        super(msg, target);
    }

    @Override
    protected String buildMessage() {
        return "The target \"" + getName() + "\" is not provided.";
    }
}
