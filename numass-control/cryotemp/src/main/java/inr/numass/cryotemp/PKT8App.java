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

import hep.dataforge.control.devices.DeviceFactory;
import hep.dataforge.meta.Meta;
import inr.numass.control.DeviceViewConnection;
import inr.numass.control.NumassControlApplication;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * @author darksnake
 */
public class PKT8App extends NumassControlApplication<PKT8Device> {
    @Override
    protected DeviceViewConnection<PKT8Device> buildView() {
        return PKT8View.build();
    }

    @Override
    protected DeviceFactory<PKT8Device> getDeviceFactory() {
        return new PKT8DeviceFactory();
    }

    @Override
    protected void setupStage(Stage stage, PKT8Device device) {
        stage.setTitle("Numass temperature view " + device.getName());
        stage.setMinHeight(400);
        stage.setMinWidth(400);
    }

    @Override
    protected boolean acceptDevice(Meta meta) {
        return Objects.equals(meta.getString("type"), "PKT8");
    }

    //    public static final String DEFAULT_CONFIG_LOCATION = "numass-devices.xml";
//
//
//    PKT8Device device;
//
//    /**
//     * @param args the command line arguments
//     */
//    public static void main(String[] args) {
//        launch(args);
//    }
//
//    @Override
//    public void start(Stage primaryStage) throws IOException, ControlException, ParseException {
////        Locale.setDefault(Locale.US);// чтобы отделение десятичных знаков было точкой
//        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
//        rootLogger.setLevel(Level.INFO);
//        new StorageManager().startGlobal();
//
//        String deviceName = getParameters().getNamed().getOrDefault("device", "PKT-8");
//
//        Meta config;
//
//        if (Boolean.parseBoolean(getParameters().getNamed().getOrDefault("debug", "false"))) {
//            config = loadTestConfig();
//        } else {
//            config = MetaFileReader.read(new File(getParameters().getNamed().getOrDefault("config", DEFAULT_CONFIG_LOCATION)));
//        }
//
//
//        device = setupDevice(deviceName, config);
//
//        // setting up storage connections
//        NumassControlUtils.connectStorage(device, config);
//
//        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PKT8Indicator.fxml"));
//        PKT8View controller = new PKT8View();
//        device.connect(controller, "view");
//        loader.setController(controller);
//
//        Parent parent = loader.load();
//
//
//        Scene scene = new Scene(parent, 400, 400);
//        primaryStage.setTitle("Numass temperature view");
//        primaryStage.setScene(scene);
//        primaryStage.setMinHeight(400);
//        primaryStage.setMinWidth(400);
////        primaryStage.setResizable(false);
//
//        primaryStage.show();
//
//        Platform.runLater(() -> {
//            try {
//                device.init();
//            } catch (ControlException e) {
//                e.printStackTrace();
//                throw new RuntimeException(e);
//            }
//        });
//    }
//
//    public Meta loadTestConfig() throws ControlException {
//        try {
//            return MetaFileReader
//                    .read(new File(getClass().getResource("/config/defaultConfig.xml").toURI()));
//        } catch (URISyntaxException | IOException | ParseException ex) {
//            throw new Error(ex);
//        }
//    }
//
//    public PKT8Device setupDevice(String deviceName, Meta config) throws ControlException {
//        Meta deviceMeta;
//
//        if (config.hasMeta("device")) {
//            deviceMeta = MetaUtils.findNodeByValue(config, "device", "name", deviceName);
//        } else {
//            deviceMeta = config;
//        }
//
//        PKT8Device device = new PKT8Device();
//        device.configure(deviceMeta);
//
//        if(!deviceMeta.hasValue(PORT_NAME_KEY)){
//            device.getLogger().warn("Port name not provided, will try to use emulation port");
//        }
//
//
//        return device;
//    }
//
//    @Override
//    public void stop() throws Exception {
//        super.stop();
//        if (device != null) {
//            device.shutdown();
//        }
//    }

}
