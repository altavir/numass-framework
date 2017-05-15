/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.fx;

import ch.qos.logback.classic.Level;
import hep.dataforge.context.Context;
import hep.dataforge.control.connections.DeviceConnection;
import hep.dataforge.control.connections.Roles;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.meta.Meta;
import inr.numass.control.DeviceViewConnection;
import inr.numass.control.NumassControlUtils;
import inr.numass.readvac.VacCollectorDevice;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * @author Alexander Nozik
 */
public class ReadVac extends Application {
    private VacCollectorDevice device;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Locale.setDefault(Locale.US);// чтобы отделение десятичных знаков было точкой
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.INFO);

        DeviceViewConnection<VacCollectorDevice> controller = buildView();

        Scene scene = new Scene(controller.getPane());

        primaryStage.setScene(scene);
        primaryStage.show();

        device = setupDevice(controller);
        primaryStage.setTitle("Numass vacuum measurements");
    }


    private VacCollectorDevice setupDevice(DeviceConnection<VacCollectorDevice> controller) {
        Meta config = NumassControlUtils.getConfig(this)
                .orElseGet(() -> NumassControlUtils.readResourceMeta("/config/devices.xml"));

        Context ctx = NumassControlUtils.setupContext(config);
        Meta mspConfig = NumassControlUtils.findDeviceMeta(config, this::acceptDevice)
                .orElseThrow(() -> new RuntimeException("Device configuration not found"));


        try {
            D d = getDeviceFactory().build(ctx, mspConfig);
            d.init();
            NumassControlUtils.connectStorage(d, config);
            Platform.runLater(() -> {
                d.connect(controller, Roles.VIEW_ROLE, Roles.DEVICE_LISTENER_ROLE);
            });
            return d;
        } catch (ControlException e) {
            throw new RuntimeException("Failed to build device", e);
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (device != null) {
            device.shutdown();
            device.getContext().close();
        }
    }


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
//            configFileName = "vac-config.xml";
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
//        Meta p1Meta = config.getMeta("p1",
//                new MetaBuilder("p1")
//                        .setValue("port", "com::/dev/ttyUSB0")
//                        .setValue("name", "P1")
//                        .build()
//        );
//
//        Sensor<Double> p1 = new MKSVacDevice(context, p1Meta);
//
//        Meta p2Meta = config.getMeta("p2",
//                new MetaBuilder("p2")
//                        .setValue("port", "tcp::192.168.111.32:4002")
//                        .setValue("name", "P2")
//                        .build()
//        );
//
//        Sensor<Double> p2 = new CM32Device(context,p2Meta);
//
//        Meta p3Meta = config.getMeta("p3",
//                new MetaBuilder("p3")
//                        .setValue("port", "tcp::192.168.111.32:4003")
//                        .setValue("name", "P3")
//                        .build()
//        );
//
//        Sensor<Double> p3 = new CM32Device(context, p3Meta);
//
//        Meta pxMeta = config.getMeta("px",
//                new MetaBuilder("px")
//                        .setValue("port", "tcp::192.168.111.32:4003")
//                        .setValue("name", "Px")
//                        .build()
//        );
//
//        Sensor<Double> px = new VITVacDevice(context,pxMeta);
//
//        Meta baratronMeta = config.getMeta("baratron",
//                new MetaBuilder("baratron")
//                        .setValue("port", "tcp::192.168.111.33:4004")
//                        .setValue("name", "Baratron")
//                        .build()
//        );
//
//        Sensor<Double> baratron = new MKSBaratronDevice(context,baratronMeta);
//
//        VacCollectorDevice collector = new VacCollectorDevice();
//        collector.configure(config);
//        collector.setSensors(p1, p2, p3, px, baratron);
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
