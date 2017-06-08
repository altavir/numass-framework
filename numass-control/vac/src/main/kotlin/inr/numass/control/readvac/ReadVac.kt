/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.control.readvac

import hep.dataforge.meta.Meta
import hep.dataforge.utils.ContextMetaFactory
import inr.numass.control.DeviceViewConnection
import inr.numass.control.NumassControlApplication
import javafx.stage.Stage

/**
 * @author Alexander Nozik
 */
class ReadVac : NumassControlApplication<VacCollectorDevice>() {
    override fun buildView(device: VacCollectorDevice): DeviceViewConnection<VacCollectorDevice> {
        return VacCollectorViewConnection()
    }

    override val deviceFactory: ContextMetaFactory<VacCollectorDevice> = VacDeviceFactory()

    override fun setupStage(stage: Stage, device: VacCollectorDevice) {
        stage.title = "Numass vacuum measurements"
    }

    override fun acceptDevice(meta: Meta): Boolean {
        return meta.getString("type", "") == "numass:vac"
    }
}



