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
package inr.numass.control.magnet;

import hep.dataforge.control.ports.Port;
import hep.dataforge.exceptions.PortException;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 *
 * @author Polina
 */
public class SafeLambdaMagnet extends LambdaMagnet {

    private final Set<SafeMagnetCondition> safeConditions = new HashSet<>();

    public SafeLambdaMagnet(String name, Port port, int address, int timeout, SafeMagnetCondition... safeConditions) {
        super(name, port, address, timeout);
        this.safeConditions.addAll(Arrays.asList(safeConditions));
    }

    public SafeLambdaMagnet(String name, Port port, int address, SafeMagnetCondition... safeConditions) {
        super(name, port, address);
        this.safeConditions.addAll(Arrays.asList(safeConditions));
    }
    
    public void addSafeCondition(Predicate<Double> condition, boolean isBlocking){
        this.safeConditions.add(new SafeMagnetCondition() {

            @Override
            public boolean isSafe(int address, double current) {
                return condition.test(current);
            }

            @Override
            public boolean isBloking() {
                return isBlocking;
            }
        });
    }

    /**
     * Add symmetric non-blocking conditions to ensure currents in two magnets have difference within given tolerance.
     * @param controller
     * @param tolerance 
     */
    public void bindTo(SafeLambdaMagnet controller, double tolerance){
        this.addSafeCondition((I)->Math.abs(controller.getMeasuredI() - I) <= tolerance, false);
        controller.addSafeCondition((I)->Math.abs(this.getMeasuredI() - I) <= tolerance, false);
    }
    
    @Override
    protected void setCurrent(double current) throws PortException {
        for (SafeMagnetCondition condition : safeConditions) {
            if (!condition.isSafe(getAddress(), current)) {
                if (condition.isBloking()) {
                    condition.onFail();
                    throw new RuntimeException("Can't set current. Condition not satisfied.");
                } else {
                    if(listener!= null){
                        listener.displayState("BOUND");
                    }
                    return;
                }
            }
        }

        super.setCurrent(current);
    }

    public interface SafeMagnetCondition {

        boolean isSafe(int address, double current);

        default boolean isBloking() {
            return true;
        }

        default void onFail() {
            LoggerFactory.getLogger(getClass()).error("Can't set current. Condition not satisfied.");
        }
    }

}
