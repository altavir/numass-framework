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

import hep.dataforge.context.Global
import hep.dataforge.goals.generate
import hep.dataforge.goals.join
import hep.dataforge.goals.pipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoalTest {
    val firstLevel = (1..10).map { index ->
        Global.generate {
            Thread.sleep(100)
            println("this is coal $index")
            "this is coal $index"
        }
    }
    val secondLevel = firstLevel.map {
        it.pipe(Global) {
            Thread.sleep(200)
            val res = it + ":Level 2"
            println(res)
            res
        }
    }
    val thirdLevel = secondLevel.map {
       it.pipe(Global) {
            Thread.sleep(300)
            val res = it.replace("Level 2", "Level 3")
            println(res)
            res
        }
    }
    val joinGoal = thirdLevel.join(Global) { Pair("joining ${it.size} elements", 10) }

    @Test
    fun testSingle() {
        assertEquals(firstLevel [3].get(), "this is coal 4")
    }

    @Test
    fun testDep() {
        assertTrue(secondLevel [3].get().endsWith("Level 2"))
    }

    @Test
    fun testJoin() {
        val (_, num) = joinGoal.get();
        assertEquals(num, 10);

    }
}