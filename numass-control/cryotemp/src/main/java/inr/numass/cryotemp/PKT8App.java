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
package inr.numass.cryotemp;

import ch.qos.logback.classic.Level;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.io.MetaFileReader;
import hep.dataforge.storage.commons.StorageManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Locale;

/**
 * @author darksnake
 */
public class PKT8App extends Application {

    PKT8MainViewController controller;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException, ControlException, ParseException {
        Locale.setDefault(Locale.US);// чтобы отделение десятичных знаков было точкой
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.INFO);
        new StorageManager().startGlobal();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PKT8MainView.fxml"));

        Parent parent = loader.load();
        controller = loader.getController();

//        Meta deviceMeta = XMLMetaConverter.fromStream(getClass().getResourceAsStream("/defaultConfig.xml"));

//        controller.setupDevice(deviceMeta);

        Scene scene = new Scene(parent, 600, 400);


        primaryStage.setTitle("PKT8 cryogenic temperature viewer");
        primaryStage.setScene(scene);
        primaryStage.setMinHeight(400);
        primaryStage.setMinWidth(600);
//        primaryStage.setResizable(false);

        primaryStage.show();

        if(getParameters().getNamed().containsKey("cfgFile")){
            controller.setConfig(MetaFileReader.read(new File(getParameters().getNamed().get("cfgFile"))));
        } else if (Boolean.parseBoolean(getParameters().getNamed().getOrDefault("debug","false"))){
            controller.loadTestConfig();
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (controller != null) {
            controller.close();
            controller = null;
        }
//        System.exit(0);
    }

}
