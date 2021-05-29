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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.goals

import org.junit.Test

import java.util.concurrent.CompletableFuture
import java.util.concurrent.FutureTask

/**
 *
 * @author Alexander Nozik
 */
class AbstractGoalTest {

    @Test
    @Throws(InterruptedException::class)
    fun testComplete() {
        val future = CompletableFuture.supplyAsync {
            try {
                Thread.sleep(500)
            } catch (ex: InterruptedException) {
                println("canceled")
                throw RuntimeException(ex)
            }

            println("finished")
            "my delayed result"
        }
        future.whenComplete { res, err ->
            println(res)
            if (err != null) {
                println(err)
            }
        }

        future.complete("my firs result")
        future.complete("my second result")
    }

    @Test
    @Throws(InterruptedException::class)
    fun testCancel() {
        val future = FutureTask {
            try {
                Thread.sleep(300)
            } catch (ex: InterruptedException) {
                println("canceled")
                throw RuntimeException(ex)
            }

            println("finished")
            "my delayed result"
        }
        future.run()
        future.cancel(true)
        Thread.sleep(500)
    }


}
