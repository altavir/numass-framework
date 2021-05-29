/*
 * Copyright  2018 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package hep.dataforge.kodex

import hep.dataforge.description.ValueDef
import hep.dataforge.listAnnotations
import hep.dataforge.states.StateDef
import hep.dataforge.states.StateDefs
import org.junit.Assert.assertEquals
import org.junit.Test

class ListAnnotationsTest {

    @StateDef(ValueDef(key = "test"))
    class Test1


    @Test
    fun testSingleAnnotation() {
        val annotations = Test1::class.java.listAnnotations(StateDef::class.java)
        assertEquals(1, annotations.size)
    }

    @StateDefs(
            StateDef(ValueDef(key = "test1")),
            StateDef(ValueDef(key = "test2"))
    )
    class Test2

    @Test
    fun testMultipleAnnotations() {
        val annotations = Test2::class.java.listAnnotations(StateDef::class.java)
        assertEquals(2, annotations.size)
    }

}