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
package inr.numass.control.magnet.fx;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import hep.dataforge.control.ports.Port;
import hep.dataforge.control.ports.PortFactory;
import hep.dataforge.exceptions.ControlException;
import inr.numass.control.magnet.MagnetController;
import inr.numass.control.magnet.SafeMagnetController;
import inr.numass.control.magnet.VirtualLambdaPort;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 *
 * @author Alexander Nozik
 */
public class MagnetControllerApp extends Application {

    Port handler;
    SafeMagnetController sourceController;
    SafeMagnetController pinchController;
    SafeMagnetController conusController;
    SafeMagnetController detectorController;
    List<SafeMagnetController> controllers = new ArrayList<>();

    @Override
    public void start(Stage stage) throws IOException, ControlException {
        Locale.setDefault(Locale.US);// чтобы отделение десятичных знаков было точкой
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        
        String logLevel = getParameters().getNamed().getOrDefault("logLevel","INFO");
        
        rootLogger.setLevel(Level.valueOf(logLevel));
        
        String logFile = getParameters().getNamed().get("logFile");
        
        if(logFile!=null){
            FileAppender<ILoggingEvent> appender = new FileAppender<>();
            appender.setFile(logFile);
            appender.setContext(rootLogger.getLoggerContext());
            appender.start();
            rootLogger.addAppender(appender);
        }
       
        String portName = getParameters().getNamed().getOrDefault("port","virtual");
        
        if(portName.equals("virtual")){
            handler = new VirtualLambdaPort("COM12", 1, 2, 3, 4);
        } else {
            handler = PortFactory.getPort(portName);
            //TODO add meta reader here
        }

        sourceController = new SafeMagnetController("SOURCE", handler, 1);
        pinchController = new SafeMagnetController("PINCH", handler, 2);
        conusController = new SafeMagnetController("CONUS", handler, 3);
        detectorController = new SafeMagnetController("DETECTOR", handler, 4);

        conusController.bindTo(pinchController, 30.0);

        controllers.add(sourceController);
        sourceController.setSpeed(4d);
        controllers.add(pinchController);
        controllers.add(conusController);
        controllers.add(detectorController);

        
        boolean showConfirmation = Boolean.parseBoolean(getParameters().getNamed().getOrDefault("confirmOut","false"));
        
        VBox vbox = new VBox(5);
        double height = 0;
        double width = 0;
        for (MagnetController controller : controllers) {
            MagnetControllerComponent comp = MagnetControllerComponent.build(controller);
            width = Math.max(width, comp.getPrefWidth());
            height += comp.getPrefHeight()+5;
            if(!showConfirmation){
                comp.setShowConfirmation(showConfirmation);
            }
            vbox.getChildren().add(comp);
        }

        Scene scene = new Scene(vbox, width, height);
        

        stage.setTitle("Numass magnet view");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop(); //To change body of generated methods, choose Tools | Templates.
        for (MagnetController magnet : controllers) {
            magnet.stopMonitorTask();
            magnet.stopUpdateTask();
        }
        if(handler.isOpen()){
            handler.close();
        }
        System.exit(0);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
