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

package hep.dataforge.names

import org.junit.Assert.assertEquals
import org.junit.Test

class NameTest {
    @Test
    fun testNameFromString() {
        val name = Name.of("first.second[28].third\\.andahalf")
        assertEquals(3, name.length.toLong())
        assertEquals("third.andahalf", name.last.unescaped)
    }

    @Test
    fun testQuery(){
        val name = Name.of("name[f = 4]")
        assertEquals("name", name.ignoreQuery().toString())
        assertEquals("f = 4", name.query)
        assertEquals("name[f = 4]",name.toString())
        assertEquals("name[f = 4]",name.unescaped)
    }

    @Test
    fun testReconstruction() {
        val name = Name.join(Name.of("first.second"), Name.ofSingle("name.with.dot"), Name.ofSingle("end[22]"))
        val str = name.toString()
        val reconstructed = Name.of(str)
        assertEquals(name, reconstructed)
        assertEquals("first", reconstructed.tokens[0].unescaped)
        assertEquals("name.with.dot", reconstructed.tokens[2].unescaped)
        assertEquals("end[22]", reconstructed.tokens[3].unescaped)
    }

    @Test
    fun testJoin() {
        val name = Name.join("first", "second", "", "another")
        assertEquals(3, name.length.toLong())
    }

}