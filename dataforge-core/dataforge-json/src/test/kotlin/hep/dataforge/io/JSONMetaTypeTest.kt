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

package hep.dataforge.io

import hep.dataforge.get
import hep.dataforge.meta.buildMeta
import org.junit.Assert.assertEquals
import org.junit.Test

class JSONMetaTypeTest {
    @Test
    fun testRead() {
        val json = """
            {
                "a" : 22,
                "b" : {
                    "c" : [1, 2, [3.1, 3.2]]
                    "d" : "my string value"
                }
            }
            """.trimMargin()
        val meta = jsonMetaType.reader.readString(json)
        assertEquals(22, meta["a"].int)
        assertEquals(3.1, meta["b.c"][2][0].double,0.01)
    }

    @Test
    fun testWrite(){
        val meta = buildMeta {
            "a" to 22
            "b" to {
                "c" to listOf(1, 2, listOf(3.1, 3.2))
                "d" to "my string value"
            }
        }
        val string = jsonMetaType.writer.writeString(meta)
        println(string)
    }
}