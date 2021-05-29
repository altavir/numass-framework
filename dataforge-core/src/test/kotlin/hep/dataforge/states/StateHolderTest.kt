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

package hep.dataforge.states

import hep.dataforge.context.Global
import hep.dataforge.values.Value
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

class StateHolderTest {

    @Test
    fun testStates() {
        val states = StateHolder()

        val results = ConcurrentHashMap<String, Value>()

        states.init(ValueState("state1"))
        states.init(ValueState("state2"))

        val subscription = states.changes()
        Global.launch {
            subscription.collect {
                println("${it.first}: ${it.second}")
                results[it.first] = it.second as Value
            }
        }

        states["state1"] = 1
        states["state2"] = 2
        states["state1"] = 3

        Thread.sleep(200)

        assertEquals(2, results["state2"]?.int)
        assertEquals(3, results["state1"]?.int)
    }
}