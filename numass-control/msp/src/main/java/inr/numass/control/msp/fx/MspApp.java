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
package inr.numass.control.msp.fx;

import ch.qos.logback.classic.Level;
import hep.dataforge.context.Global;
import hep.dataforge.io.MetaFileReader;
import hep.dataforge.io.XMLMetaReader;
import hep.dataforge.meta.Meta;
import hep.dataforge.storage.commons.StorageManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Locale;

/**
 *
 * @author darksnake
 */
public class MspApp extends Application {

    MspViewController controller;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Locale.setDefault(Locale.US);// чтобы отделение десятичных знаков было точкой
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.INFO);
        new StorageManager().startGlobal();

        String configFileName = getParameters().getNamed().get("config");
        if (configFileName == null) {
            configFileName = "msp-config.xml";
        }
        File configFile = new File(configFileName);
        Meta config;
        if (configFile.exists()) {
            config = MetaFileReader.read(configFile).build();
        } else {
//            throw new RuntimeException("Configuration file not found");
            config = new XMLMetaReader().read(MspApp.class.getClassLoader().getResourceAsStream("config/msp-config.xml"), -1, null);
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MspView.fxml"));

        Parent parent = loader.load();
        controller = loader.getController();

        controller.setDeviceConfig(Global.instance(), config);

        Scene scene = new Scene(parent, 600, 400);

        primaryStage.setTitle("Numass mass-spectrometer view");
        primaryStage.setScene(scene);
        primaryStage.setMinHeight(400);
        primaryStage.setMinWidth(600);
//        primaryStage.setResizable(false);

        primaryStage.show();

    }

    @Override
    public void stop() throws Exception {
        super.stop();
        controller.shutdown();
//        System.exit(0);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
