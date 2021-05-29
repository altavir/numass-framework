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
package hep.dataforge.meta

/**
 * A general convention on object with meta-data
 *
 * @author Alexander Nozik
 */
interface Metoid {

    /**
     * Get the meta-data for this object. By convention null is not allowed. If
     * there is no meta-data, empty meta is returned. The name of returned meta
     * is currently not restricted.
     *
     * @since 0.4.0
     * @return
     */
    val meta: Meta
}
