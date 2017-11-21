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
 * @author Polina
 */
class MagnetStatus(
        /**
         * @return the isOut
         */
        val isOutputOn: Boolean,
        /**
         * @return the measuredCurrent
         */
        val measuredCurrent: Double,
        /**
         * @return the setCurrent
         */
        val setCurrent: Double,
        /**
         * @return the measuredVoltage
         */
        val measuredVoltage: Double,
        /**
         * @return the setVoltage
         */
        val setVoltage: Double) {

    /**
     * @return the isOn
     */
    var isOn: Boolean = false
        private set

    init {
        this.isOn = true
    }

    companion object {

        fun off(): MagnetStatus {
            val res = MagnetStatus(false, 0.0, 0.0, 0.0, 0.0)
            res.isOn = false
            return res
        }
    }

}
