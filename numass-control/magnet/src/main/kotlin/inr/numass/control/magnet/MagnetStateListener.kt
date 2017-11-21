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
package inr.numass.control.magnet

/**
 *
 * @author Alexander Nozik
 */
interface MagnetStateListener {

    fun acceptStatus(name: String, state: MagnetStatus)

    fun acceptNextI(name: String, nextI: Double)

    fun acceptMeasuredI(name: String, measuredI: Double)

    open fun displayState(state: String) {

    }

    open fun error(name: String, errorMessage: String, throwable: Throwable?) {
        throw RuntimeException(errorMessage, throwable)
    }

    open fun monitorTaskStateChanged(name: String, monitorTaskRunning: Boolean) {

    }

    open fun updateTaskStateChanged(name: String, updateTaskRunning: Boolean) {

    }

    open fun outputModeChanged(name: String, out: Boolean) {

    }

    fun addressChanged(name: String, address: Int) {

    }

}
