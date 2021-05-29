/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.plots.viewer

import tornadofx.*

/**

 * @author Alexander Nozik
 */
class ViewerApp : App(PlotView::class) {

//    override fun start(stage: Stage) {
//
//        val loader = FXMLLoader(javaClass.getResource("/fxml/ViewerApp.fxml"))
//        val root = loader.load<Parent>()
//        val controller = loader.getController<PlotView>()
//        val scene = Scene(root)
//
//        stage.title = "DataForge plot viewer"
//        stage.scene = scene
//        stage.show()
//
//        for (fileName in this.parameters.unnamed) {
//            controller.loadPlot(File(fileName))
//        }
//    }
}
