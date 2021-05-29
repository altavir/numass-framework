/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.control.readvac

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaUtils
import inr.numass.control.NumassControlApplication
import javafx.stage.Stage

/**
 * @author Alexander Nozik
 */
class ReadVac : NumassControlApplication<VacCollectorDevice>() {

    override val deviceFactory = VacDeviceFactory()

    override fun setupStage(stage: Stage, device: VacCollectorDevice) {
        stage.title = "Numass vacuum measurements"
    }

    override fun getDeviceMeta(config: Meta): Meta {
        return MetaUtils.findNode(config, "device") {
            it.getString("type") == "numass:vac"
        }.orElseThrow {
            RuntimeException("Vacuum measurement configuration not found")
        }
    }
}



