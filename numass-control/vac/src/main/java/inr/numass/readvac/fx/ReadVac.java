/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.fx;

import hep.dataforge.control.devices.DeviceFactory;
import hep.dataforge.meta.Meta;
import inr.numass.control.DeviceViewConnection;
import inr.numass.control.NumassControlApplication;
import inr.numass.readvac.VacCollectorDevice;
import inr.numass.readvac.VacDeviceFactory;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * @author Alexander Nozik
 */
public class ReadVac extends NumassControlApplication<VacCollectorDevice> {
    @Override
    protected DeviceViewConnection<VacCollectorDevice> buildView() {
        return VacCollectorView.build();
    }

    @Override
    protected DeviceFactory<VacCollectorDevice> getDeviceFactory() {
        return new VacDeviceFactory();
    }

    @Override
    protected void setupStage(Stage stage, VacCollectorDevice device) {
        stage.setTitle("Numass vacuum measurements");
    }

    @Override
    protected boolean acceptDevice(Meta meta) {
        return Objects.equals(meta.getString("type", ""), "numass:vac");
    }
//    private VacCollectorDevice device;
//
//    @Override
//    public void start(Stage primaryStage) throws Exception {
//        Locale.setDefault(Locale.US);// чтобы отделение десятичных знаков было точкой
//        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
//        rootLogger.setLevel(Level.INFO);
//
//        DeviceViewConnection<VacCollectorDevice> controller = buildView();
//
//        Scene scene = new Scene(controller.getPane());
//
//        primaryStage.setScene(scene);
//        primaryStage.show();
//
//        device = setupDevice(controller);
//        primaryStage.setTitle("Numass vacuum measurements");
//    }
//
//
//    private VacCollectorDevice setupDevice(DeviceConnection<VacCollectorDevice> controller) {
//        Meta config = NumassControlUtils.getConfig(this)
//                .orElseGet(() -> NumassControlUtils.readResourceMeta("/config/devices.xml"));
//
//        Context ctx = NumassControlUtils.setupContext(config);
//        Meta mspConfig = NumassControlUtils.findDeviceMeta(config, this::acceptDevice)
//                .orElseThrow(() -> new RuntimeException("Device configuration not found"));
//
//
//        try {
//            D d = getDeviceFactory().build(ctx, mspConfig);
//            d.init();
//            NumassControlUtils.connectStorage(d, config);
//            Platform.runLater(() -> {
//                d.connect(controller, Roles.VIEW_ROLE, Roles.DEVICE_LISTENER_ROLE);
//            });
//            return d;
//        } catch (ControlException e) {
//            throw new RuntimeException("Failed to build device", e);
//        }
//    }
//
//    @Override
//    public void stop() throws Exception {
//        super.stop();
//        if (device != null) {
//            device.shutdown();
//            device.getContext().close();
//        }
//    }


//    VacCollectorView controller;
//    Logger logger = LoggerFactory.getLogger("ReadVac");
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
//        String configFileName = getParameters().getNamed().get("config");
//        if (configFileName == null) {
//            configFileName = "devices.xml";
//        }
//        File configFile = new File(configFileName);
//        Meta config;
//        if (configFile.exists()) {
//            config = MetaFileReader.read(configFile).build();
//        } else {
//            config = Meta.empty();
//        }
//
//        Context context = Global.instance();
//

//        collector.init();
//
//        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/VacCollector.fxml"));
//        loader.load();
//        controller = loader.getController();
//        controller.setDevice(collector);
//        controller.setLogger(logger);
//
//        controller.setLoaderFactory((VacCollectorDevice device, Storage localStorage) -> {
//            try {
//                String runName = ClientUtils.getRunName(config);
////                String runName = device.meta().getString("numass.run", "");
////                if (config.hasMeta("numass.server")) {
////                    try {
////                        logger.info("Obtaining run information from cetral server...");
////                        NumassClient client = new NumassClient(get);
////                        runName = client.getCurrentRun().getString("path", "");
////                        logger.info("Run name is '{}'", runName);
////                    } catch (Exception ex) {
////                        logger.warn("Failed to download current run information", ex);
////                    }
////                }
//
//                TableFormatBuilder format = new TableFormatBuilder().setType("timestamp", ValueType.TIME);
//                device.getSensors().stream().forEach((s) -> {
//                    format.setType(s.getName(), ValueType.NUMBER);
//                });
//
//                PointLoader pl = LoaderFactory.buildPointLoder(localStorage, "vactms", runName, "timestamp", format.build());
//                return pl;
//
//            } catch (StorageException ex) {
//                throw new RuntimeException(ex);
//            }
//        });
//        Scene scene = new Scene(loader.getRoot(), 800, 700);
//
//        primaryStage.setTitle("Numass vacuum measurements");
//        primaryStage.setScene(scene);
//        primaryStage.show();
////        controller.startMeasurement();
//    }
//
//    @Override
//    public void stop() throws Exception {
//        if (controller != null) {
//            controller.stopMeasurement();
//            controller.getDevice().shutdown();
//        }
//        super.stop();
//    }

}
