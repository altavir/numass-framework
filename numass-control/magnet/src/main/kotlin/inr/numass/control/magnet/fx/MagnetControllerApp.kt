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
package inr.numass.control.magnet.fx

import tornadofx.*

/**
 *
 * @author Alexander Nozik
 */
class MagnetControllerApp : App() {

//
//    private lateinit var device: LambdaHub
//
//    @Throws(IOException::class, ControlException::class)
//    override fun start(stage: Stage) {
//        Locale.setDefault(Locale.US)// чтобы отделение десятичных знаков было точкой
//        val rootLogger = LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
//
//        val logLevel = parameters.named.getOrDefault("logLevel", "INFO")
//
//        rootLogger.level = Level.valueOf(logLevel)
//
//        val logFile = parameters.named["logFile"]
//
//        if (logFile != null) {
//            val appender = FileAppender<ILoggingEvent>()
//            appender.file = logFile
//            appender.context = rootLogger.loggerContext
//            appender.start()
//            rootLogger.addAppender(appender)
//        }
//
//
//        val configFileName = parameters.named
//        val config = MetaFileReader.instance().read(context,)
//
////        val portName = (parameters.named as java.util.Map<String, String>).getOrDefault("port", "virtual")
////
////        if (portName == "virtual") {
////            handler = VirtualLambdaPort("COM12", 1, 2, 3, 4)
////        } else {
////            handler = PortFactory.getPort(portName)
////            //TODO add meta reader here
////        }
////
////        sourceController = SafeLambdaMagnet("SOURCE", handler, 1)
////        pinchController = SafeLambdaMagnet("PINCH", handler, 2)
////        conusController = SafeLambdaMagnet("CONUS", handler, 3)
////        detectorController = SafeLambdaMagnet("DETECTOR", handler, 4)
////
////        conusController.bindTo(pinchController, 30.0)
////
////        controllers.add(sourceController)
////        sourceController.speed = 4.0
////        controllers.add(pinchController)
////        controllers.add(conusController)
////        controllers.add(detectorController)
//
//
//        val showConfirmation = java.lang.Boolean.parseBoolean((parameters.named as java.util.Map<String, String>).getOrDefault("confirmOut", "false"))
//
//        val vbox = VBox(5.0)
//        var height = 0.0
//        var width = 0.0
//        for (controller in controllers) {
//            val comp = MagnetControllerComponent.build(controller)
//            width = Math.max(width, comp.prefWidth)
//            height += comp.prefHeight + 5
//            if (!showConfirmation) {
//                comp.setShowConfirmation(showConfirmation)
//            }
//            vbox.children.add(comp)
//        }
//
//        val scene = Scene(vbox, width, height)
//
//
//        stage.title = "Numass magnet view"
//        stage.scene = scene
//        stage.isResizable = false
//        stage.show()
//    }
//
//    @Throws(Exception::class)
//    override fun stop() {
//        super.stop() //To change body of generated methods, choose Tools | Templates.
//        for (magnet in controllers) {
//            magnet.stopMonitorTask()
//            magnet.stopUpdateTask()
//        }
//        if (handler.isOpen) {
//            handler.close()
//        }
//        System.exit(0)
//    }
//
//    companion object {
//
//        /**
//         * @param args the command line arguments
//         */
//        @JvmStatic
//        fun main(args: Array<String>) {
//            Application.launch(*args)
//        }
//    }

}
