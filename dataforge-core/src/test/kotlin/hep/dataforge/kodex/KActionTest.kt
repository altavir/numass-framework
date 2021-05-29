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

import hep.dataforge.actions.KPipe
import hep.dataforge.context.Global
import hep.dataforge.data.DataSet
import hep.dataforge.meta.Meta
import hep.dataforge.meta.buildMeta
import hep.dataforge.workspace.BasicWorkspace
import hep.dataforge.workspace.tasks.task
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.time.delay
import org.junit.Test
import java.time.Duration

class KActionTest {
    val data: DataSet<String> = DataSet.edit(String::class.java).apply {
        (1..10).forEach {
            putData("$it", "this is my data $it", buildMeta { "index" to it });
        }
    }.build()

    val pipe = KPipe("testPipe", String::class.java, String::class.java) {
        name = "newName_${meta.getInt("index")}"
        if (meta.getInt("index") % 2 == 0) {
            meta.putValue("odd", true);
        }
        result {
            println("performing action on $name")
            delay(Duration.ofMillis(400));
            it + ": stage1";
        }
    }

    @Test
    fun testPipe() {
        println("test pipe")
        val res = pipe.run(Global, data, Meta.empty());
        val datum = res.getData("newName_4")
        val value = datum.goal.get()
        assertTrue(datum.meta.getBoolean("odd"))
        assertEquals("this is my data 4: stage1", value)
    }

    @Test
    fun testPipeTask() {
        println("test pipe task")

        val testTask = task("test") {
            model {
                data("*")
            }
            action(pipe)
        }



        Global.setValue("cache.enabled", false)
        val workspace = BasicWorkspace.builder()
                .data("test", data)
                .task(testTask)
                .target("test", Meta.empty())
                .apply { context = Global}
                .build()

        val res = workspace.runTask("test")

        val datum = res.getData("newName_4")
        val value = datum.goal.get()
        assertTrue(datum.meta.getBoolean("odd"))
        assertEquals("this is my data 4: stage1", value)

    }
}