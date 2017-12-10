/*
 * Copyright  2017 Alexander Nozik.
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

package inr.numass.scripts.temp

import hep.dataforge.kodex.*
import hep.dataforge.meta.Meta
import hep.dataforge.workspace.BasicWorkspace
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) {
    runBlocking {


        val action = KPipe(
                name = "test",
                inType = String::class.java,
                outType = String::class.java,
                action = {
                    result {
                        Thread.sleep(300)
                        "the result is $it"
                    }
                }
        )

        val testTask = task("test") {
            model { meta ->
                data("static");
            }
            action(action)
        }

        GLOBAL.setValue("cache.enabled", false)

        val workspace = BasicWorkspace.Builder()
                .setContext(GLOBAL)
                .staticData("static", "22")
                .task(testTask)
                .target(buildMeta("test"))
                .build()



        val resData = workspace
                .runTask("test", "test")
                .getData("static")

        val taskRes = resData.goal.await()
        println(taskRes)


        val actionRes = action.run(GLOBAL, workspace.data.checked(String::class.java), Meta.empty())

        println(actionRes.getData("static").goal.await())

        GLOBAL.close()

//        val res = static.pipe<Int, String>(GLOBAL.coroutineContext) {
//            Thread.sleep(300)
//            "the result is $it"
//        }
//        println(res.goal.await())
    }
}
