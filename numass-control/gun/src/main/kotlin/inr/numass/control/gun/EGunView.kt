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

package inr.numass.control.gun

import hep.dataforge.fx.asBooleanProperty
import hep.dataforge.fx.asDoubleProperty
import inr.numass.control.DeviceDisplayFX
import inr.numass.control.indicator
import javafx.geometry.Orientation
import tornadofx.*

class GunDisplay: DeviceDisplayFX<EGun>(){
    override fun buildView(device: EGun): UIComponent? {
        return EGunView(device)
    }
}


class EGunView(val gun: EGun) : View() {
    override val root = borderpane {
        top{
            buttonbar {
                button("refresh"){
                    action {
                        gun.sources.forEach {
                            it.update()
                        }
                    }
                }
            }
        }
        center {
            vbox {
                gun.sources.forEach { source ->
                    hbox {
                        label(source.name){
                            minWidth = 100.0
                        }
                        separator(Orientation.VERTICAL)

                        indicator {
                            bind(source.connectedState.asBooleanProperty())
                        }

                        val voltageProperty = source.voltageState.asDoubleProperty()
                        val currentProperty = source.currentState.asDoubleProperty()

                        textfield {

                        }

                        separator(Orientation.VERTICAL)
                        label("V: ")
                        label(voltageProperty)

                        separator(Orientation.VERTICAL)

                        label("I: ")
                        label(currentProperty)
                    }
                }
            }
        }
    }
}