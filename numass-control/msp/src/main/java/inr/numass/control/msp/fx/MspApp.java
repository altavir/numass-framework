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

import hep.dataforge.control.devices.DeviceFactory;
import inr.numass.control.DeviceViewConnection;
import inr.numass.control.NumassControlApplication;
import inr.numass.control.msp.MspDevice;
import inr.numass.control.msp.MspDeviceFactory;
import javafx.stage.Stage;

/**
 * @author darksnake
 */
public class MspApp extends NumassControlApplication<MspDevice> {

    @Override
    protected DeviceViewConnection<MspDevice> buildView() {
        return MspViewController.build();
    }

    @Override
    protected DeviceFactory<MspDevice> getDeviceFactory() {
        return new MspDeviceFactory();
    }

    @Override
    protected void setupStage(Stage stage) {
        stage.setTitle("Numass mass-spectrometer view");
        stage.setMinHeight(400);
        stage.setMinWidth(600);
    }

//    private Device device;
//
//
//    /**
//     * @param args the command line arguments
//     */
//    public static void main(String[] args) {
//        launch(args);
//    }
//
//    @Override
//    public void start(Stage primaryStage) throws Exception {
//        Locale.setDefault(Locale.US);// чтобы отделение десятичных знаков было точкой
//        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
//        rootLogger.setLevel(Level.INFO);
//
//        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MspView.fxml"));
//
//        Parent parent = loader.load();
//        MspViewController controller = loader.getController();
//
//        Scene scene = new Scene(parent, 800, 600);
//
//        primaryStage.setTitle("Numass mass-spectrometer view");
//        primaryStage.setScene(scene);
//        primaryStage.setMinHeight(400);
//        primaryStage.setMinWidth(600);
//
//        primaryStage.show();
//
//        setupDevice(controller);
//    }
//
//    private void setupDevice(MspViewController controller){
//        Meta config = NumassControlUtils.getConfig(this)
//                .orElseGet(() ->  NumassControlUtils.readResourceMeta("/config/msp-config.xml"));
//
//        Context ctx = NumassControlUtils.setupContext(config);
//        Meta mspConfig = NumassControlUtils.findDeviceMeta(config,it-> Objects.equals(it.getString("name"), "msp"))
//                .orElseThrow(()-> new RuntimeException("Msp configuration not found"));
//
//
//        Platform.runLater(() -> {
//            try {
//                device = new MspDeviceFactory().build(ctx, mspConfig);
//                device.init();
//                device.connect(controller, Roles.VIEW_ROLE);
//                NumassControlUtils.connectStorage(device,config);
//            } catch (ControlException e) {
//                throw new RuntimeException("Failed to build device", e);
//            }
//        });
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
