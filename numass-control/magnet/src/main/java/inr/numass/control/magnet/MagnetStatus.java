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

/**
 *
 * @author Polina
 */
public class MagnetStatus {

    private boolean on;
    private final boolean out;
    private final double measuredCurrent;
    private final double setCurrent;
    private final double measuredVoltage;
    private final double setVoltage;

    public MagnetStatus(boolean isOut, double measuredCurrent, double setCurrent, double measuredVoltage, double setVoltage) {
        this.on = true;
        this.out = isOut;
        this.measuredCurrent = measuredCurrent;
        this.setCurrent = setCurrent;
        this.measuredVoltage = measuredVoltage;
        this.setVoltage = setVoltage;
    }

    public static MagnetStatus off() {
        MagnetStatus res = new MagnetStatus(false, 0, 0, 0, 0);
        res.on = false;
        return res;
    }

    /**
     * @return the isOn
     */
    public boolean isOn() {
        return on;
    }

    /**
     * @return the isOut
     */
    public boolean isOutputOn() {
        return out;
    }

    /**
     * @return the measuredCurrent
     */
    public double getMeasuredCurrent() {
        return measuredCurrent;
    }

    /**
     * @return the setCurrent
     */
    public double getSetCurrent() {
        return setCurrent;
    }

    /**
     * @return the measuredVoltage
     */
    public double getMeasuredVoltage() {
        return measuredVoltage;
    }

    /**
     * @return the setVoltage
     */
    public double getSetVoltage() {
        return setVoltage;
    }

}
