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

import java.lang.annotation.*;

/**
 * <p>
 * NodeDef class.</p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(NodeDefs.class)
public @interface NodeDef {

    String key();

    String info() default "";

    boolean multiple() default false;

    boolean required() default false;

    String[] tags() default {};

    /**
     * A list of child value descriptors
     */
    ValueDef[] values() default {};

    /**
     * A target class for this node to describe
     * @return
     */
    Class type() default Object.class;

    /**
     * The DataForge path to the resource containing the description. Following targets are supported:
     * <ol>
     *     <li>resource</li>
     *     <li>file</li>
     *     <li>class</li>
     *     <li>method</li>
     *     <li>property</li>
     * </ol>
     *
     * Does not work if [type] is provided
     *
     * @return
     */
    String descriptor() default "";
}
