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
import hep.dataforge.control.connections.Roles;
import hep.dataforge.control.connections.StorageConnection;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.exceptions.StorageException;
import hep.dataforge.io.MetaFileReader;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaUtils;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.commons.StorageFactory;
import hep.dataforge.storage.commons.StorageManager;
import inr.numass.client.ClientUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

/**
 * @author darksnake
 */
public class PKT8App extends Application {
    public static final String DEFAULT_CONFIG_LOCATION = "numass-devices.xml";


    PKT8Device device;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException, ControlException, ParseException {
//        Locale.setDefault(Locale.US);// чтобы отделение десятичных знаков было точкой
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.INFO);
        new StorageManager().startGlobal();

        String deviceName = getParameters().getNamed().getOrDefault("device", "PKT-8");

        Meta config;

        if (Boolean.parseBoolean(getParameters().getNamed().getOrDefault("debug", "false"))) {
            config = loadTestConfig();
        } else {
            config = MetaFileReader.read(new File(getParameters().getNamed().getOrDefault("cfgFile", DEFAULT_CONFIG_LOCATION)));
        }


        device = setupDevice(deviceName, config);

        // setting up storage connections
        if (config.hasMeta("storage")) {
            String numassRun = ClientUtils.getRunName(config);
            config.getMetaList("storage").forEach(node -> {
                Storage storage = StorageFactory.buildStorage(device.getContext(), node);
                if (!numassRun.isEmpty()) {
                    try {
                        storage = storage.buildShelf(numassRun, Meta.empty());
                    } catch (StorageException e) {
                        LoggerFactory.getLogger(getClass()).error("Failed to build shelf");
                    }
                }
                device.connect(new StorageConnection(storage), Roles.STORAGE_ROLE);
            });
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PKT8Indicator.fxml"));
        PKT8Controller controller = new PKT8Controller(device);
        loader.setController(controller);

        Parent parent = loader.load();


        Scene scene = new Scene(parent, 400, 400);
        primaryStage.setTitle("Numass temperature view");
        primaryStage.setScene(scene);
        primaryStage.setMinHeight(400);
        primaryStage.setMinWidth(400);
//        primaryStage.setResizable(false);

        primaryStage.show();

        Platform.runLater(() -> {
            try {
                device.init();
//                controller.start();
            } catch (ControlException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    public Meta loadTestConfig() throws ControlException {
        try {
            return MetaFileReader
                    .read(new File(getClass().getResource("/config/defaultConfig.xml").toURI()));
        } catch (URISyntaxException | IOException | ParseException ex) {
            throw new Error(ex);
        }
    }

    public PKT8Device setupDevice(String deviceName, Meta config) throws ControlException {
        Meta deviceMeta;

        if (config.hasMeta("device")) {
            deviceMeta = MetaUtils.findNodeByValue(config, "device", "name", deviceName);
        } else {
            deviceMeta = config;
        }

        PKT8Device device = new PKT8Device(deviceMeta.getString("port", "virtual"));

        device.configure(deviceMeta);

        return device;
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (device != null) {
            device.shutdown();
        }
    }

}
