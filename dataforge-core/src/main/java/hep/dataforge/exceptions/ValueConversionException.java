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

import hep.dataforge.values.Value;
import hep.dataforge.values.ValueType;

/**
 * <p>ValueConversionException class.</p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class ValueConversionException extends RuntimeException {

    Value value;
    ValueType to;


    /**
     * <p>Constructor for ValueConversionException.</p>
     *
     * @param value a {@link hep.dataforge.values.Value} object.
     * @param to    a {@link hep.dataforge.values.ValueType} object.
     */
    public ValueConversionException(Value value, ValueType to) {
        this.value = value;
        this.to = to;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage() {
        return String.format("Failed to convert value '%s' of type %s to %s", value.getString(), value.getType(), to.name());
    }


}
