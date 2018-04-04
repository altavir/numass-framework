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

import hep.dataforge.context.Context
import hep.dataforge.exceptions.PortException
import hep.dataforge.meta.Meta
import org.slf4j.LoggerFactory
import java.util.*

/**
 *
 * @author Polina
 */
class SafeLambdaMagnet(context: Context, meta: Meta, controller: LambdaPortController) :
        LambdaMagnet(context, meta, controller) {

    private val safeConditions = HashSet<SafeMagnetCondition>()

    //    public SafeLambdaMagnet(String name, Port port, int address, int timeout, SafeMagnetCondition... safeConditions) {
    //        super(name, port, address, timeout);
    //        this.safeConditions.addAll(Arrays.asList(safeConditions));
    //    }
    //
    //    public SafeLambdaMagnet(String name, Port port, int address, SafeMagnetCondition... safeConditions) {
    //        super(name, port, address);
    //        this.safeConditions.addAll(Arrays.asList(safeConditions));
    //    }

    fun addSafeCondition(isBlocking: Boolean, condition: (Double) -> Boolean) {
        this.safeConditions.add(object : SafeMagnetCondition {

            override fun isBlocking(): Boolean = isBlocking

            override fun isSafe(address: Int, current: Double): Boolean = condition(current)
        })
    }

    /**
     * Add symmetric non-blocking conditions to ensure currents in two magnets have difference within given tolerance.
     * @param controller
     * @param tolerance
     */
    fun bindTo(controller: SafeLambdaMagnet, tolerance: Double) {
        this.addSafeCondition(false) { I -> Math.abs(controller.current - I) <= tolerance }
        controller.addSafeCondition(false) { I -> Math.abs(this.current - I) <= tolerance }
    }


    @Throws(PortException::class)
    override fun setCurrent(current: Double) {
        safeConditions
                .filterNot { it.isSafe(address, current) }
                .forEach {
                    if (it.isBlocking()) {
                        it.onFail()
                        throw RuntimeException("Can't set current. Condition not satisfied.")
                    } else {
                        listener?.displayState("BOUND")
                    }
                }

        super.current = current
    }

    interface SafeMagnetCondition {

        fun isBlocking(): Boolean = true

        fun isSafe(address: Int, current: Double): Boolean

        fun onFail() {
            LoggerFactory.getLogger(javaClass).error("Can't set current. Condition not satisfied.")
        }
    }

}
