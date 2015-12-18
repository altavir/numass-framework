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
package inr.numass.server;

/**
 *
 * @author Alexander Nozik
 */
public class UnknownNumassActionException extends RuntimeException {

    public enum Cause {
        IN_DEVELOPMENT,
        NOT_SUPPORTED,
        DEPRECATED
    }

    private final String command;
    private final Cause cause;

    /**
     * Creates a new instance of <code>UnknownCommandException</code> without
     * detail message.
     */
    public UnknownNumassActionException(String command, Cause cause) {
        this.command = command;
        this.cause = cause;
    }

    @Override
    public String getMessage() {
        return String.format("Can't run the action '%s' because it is %s", command, cause.name());
    }

}
