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

import hep.dataforge.kodex.GLOBAL
import hep.dataforge.kodex.await
import hep.dataforge.workspace.FileBasedWorkspace
import kotlinx.coroutines.experimental.runBlocking
import java.io.File

fun main(args: Array<String>) {
    val numass = FileBasedWorkspace.build(GLOBAL, File("D:\\Work\\Numass\\sterile2017_05\\workspace.groovy").toPath())
    runBlocking {
        val res = numass
                .runTask("analyze", "test")
                .getData("Fill_2.set_2").goal.await()
        println(res)
    }
}
