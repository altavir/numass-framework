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
import hep.dataforge.control.connections.Roles;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.io.MetaFileReader;
import hep.dataforge.io.XMLMetaReader;
import hep.dataforge.meta.Meta;
import hep.dataforge.storage.commons.StorageManager;
import inr.numass.control.msp.MspDevice;
import inr.numass.control.msp.MspDeviceFactory;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Locale;

import static inr.numass.control.msp.MspDevice.MSP_DEVICE_TYPE;

/**
 * @author darksnake
 */
public class MspApp extends Application {
    public static final String DEFAULT_CONFIG_LOCATION = "msp-config.xml";

    private MspDevice device;


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Locale.setDefault(Locale.US);// чтобы отделение десятичных знаков было точкой
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.INFO);
        new StorageManager().startGlobal();

        String configFileName = getParameters().getNamed().get("config");
        if (configFileName == null) {
            configFileName = DEFAULT_CONFIG_LOCATION;
        }
        File configFile = new File(configFileName);
        Meta config;
        if (configFile.exists()) {
            config = MetaFileReader.read(configFile).build();
        } else {
            config = new XMLMetaReader().read(getClass().getResourceAsStream("/config/msp-config.xml"));
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MspView.fxml"));

        Parent parent = loader.load();
        MspViewController controller = loader.getController();

        Scene scene = new Scene(parent, 800, 600);

        primaryStage.setTitle("Numass mass-spectrometer view");
        primaryStage.setScene(scene);
        primaryStage.setMinHeight(400);
        primaryStage.setMinWidth(600);

        Platform.runLater(()->{
            try {
                device = new MspDeviceFactory().build(Global.instance(), getMspConfig(config));
                device.init();
                device.connect(controller, Roles.VIEW_ROLE);
            } catch (ControlException e) {
                throw new RuntimeException("Failed to build device", e);
            }
        });
        primaryStage.show();
    }

    private Meta getMspConfig(Meta config) {
        Meta mspConfig = null;
        if (config.hasMeta("device")) {
            for (Meta d : config.getMetaList("device")) {
                if (d.getString("type", "unknown").equals(MSP_DEVICE_TYPE)) {
                    mspConfig = d;
                }
            }
        } else if (config.hasMeta("peakJump")) {
            mspConfig = config;
        }
        return mspConfig;
    }


//                showError(String.format("Can't connect to %s:%d. The port is either busy or not the MKS mass-spectrometer port",
//                        device.meta().getString("connection.ip", "127.0.0.1"),
//                        device.meta().getInt("connection.port", 10014)));
//                throw new RuntimeException("Can't connect to device");


    @Override
    public void stop() throws Exception {
        super.stop();
        if (device != null) {
            device.shutdown();
        }
    }

}
