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
package hep.dataforge.description;

import hep.dataforge.values.ValueType;

import java.lang.annotation.*;

/**
 * Декларация параметра аннотации, который исползуется в контенте или методе,
 * параметром которого является аннотация
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(ValueDefs.class)
public @interface ValueDef {

    String key();

    ValueType[] type() default {ValueType.STRING};

    boolean multiple() default false;

    String def() default "";

    String info() default "";

    boolean required() default true;

    String[] allowed() default {};

    Class enumeration() default Object.class;

    String[] tags() default {};
}
