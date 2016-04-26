/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.app;

import hep.dataforge.control.measurements.Sensor;
import hep.dataforge.exceptions.StorageException;
import hep.dataforge.io.MetaFileReader;
import hep.dataforge.meta.Meta;
import hep.dataforge.tables.TableFormatBuilder;
import hep.dataforge.storage.api.PointLoader;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.commons.LoaderFactory;
import hep.dataforge.values.ValueType;
import inr.numass.client.NumassClient;
import inr.numass.readvac.devices.CM32Device;
import inr.numass.readvac.devices.MKSBaratronDevice;
import inr.numass.readvac.devices.MKSVacDevice;
import inr.numass.readvac.devices.VITVacDevice;
import inr.numass.readvac.devices.VacCollectorDevice;
import inr.numass.readvac.fx.VacCollectorController;
import java.io.File;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Alexander Nozik
 */
public class ReadVac extends Application {

    VacCollectorController controller;
    Logger logger = LoggerFactory.getLogger("ReadVac");

    @Override
    public void start(Stage primaryStage) throws Exception {
        String configFileName = getParameters().getNamed().get("config");
        if (configFileName == null) {
            configFileName = "vac-config.xml";
        }
        File configFile = new File(configFileName);
        Meta config;
        if (configFile.exists()) {
            config = MetaFileReader.read(configFile).build();
        } else {
            config = Meta.empty();
        }

        Sensor<Double> p1 = new MKSVacDevice(config.getString("p1.port", "com::/dev/ttyUSB0"));
        p1.configure(config.getNode("p1", Meta.empty()));
        p1.setName(config.getString("p1.name", "P1"));
        Sensor<Double> p2 = new CM32Device(config.getString("p2.port", "tcp::192.168.111.32:4002"));
        p2.configure(config.getNode("p2", Meta.empty()));
        p2.setName(config.getString("p2.name", "P2"));
        Sensor<Double> p3 = new CM32Device(config.getString("p3.port", "tcp::192.168.111.32:4003"));
        p3.configure(config.getNode("p3", Meta.empty()));
        p3.setName(config.getString("p3.name", "P3"));
        Sensor<Double> px = new VITVacDevice(config.getString("px.port", "com::/dev/ttyUSB1"));
        px.configure(config.getNode("px", Meta.empty()));
        px.setName(config.getString("px.name", "Px"));
        Sensor<Double> baratron = new MKSBaratronDevice(config.getString("baratron.port", "tcp::192.168.111.33:4004"));
        baratron.configure(config.getNode("baratron", Meta.empty()));
        baratron.setName(config.getString("baratron.name", "Baratron"));

        VacCollectorDevice collector = new VacCollectorDevice();
        collector.configure(config);
        collector.setSensors(p1, p2, p3, px, baratron);
        collector.init();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/VacCollector.fxml"));
        loader.load();
        controller = loader.getController();
        controller.setDevice(collector);
        controller.setLogger(logger);

        controller.setLoaderFactory((VacCollectorDevice device, Storage localStorage) -> {
            try {
                String runName = device.meta().getString("numass.run", "");
                if (config.hasNode("numass")) {
                    try {
                        logger.info("Obtaining run information from cetral server...");
                        NumassClient client = new NumassClient(config.getString("numass.ip", "192.168.111.1"),
                                config.getInt("numass.port", 8335));
                        runName = client.getCurrentRun().getString("path", "");
                        logger.info("Run name is '{}'", runName);
                    } catch (Exception ex) {
                        logger.warn("Failed to download current run information", ex);
                    }
                }

                TableFormatBuilder format = new TableFormatBuilder().setType("timestamp", ValueType.TIME);
                device.getSensors().stream().forEach((s) -> {
                    format.setType(s.getName(), ValueType.NUMBER);
                });

                PointLoader pl = LoaderFactory.buildPointLoder(localStorage, "vactms", runName, "timestamp", format.build());
                return pl;

            } catch (StorageException ex) {
                throw new RuntimeException(ex);
            }
        });
        Scene scene = new Scene(loader.getRoot(), 800, 700);

        primaryStage.setTitle("Numass vacuum measurements");
        primaryStage.setScene(scene);
        primaryStage.show();
//        controller.startMeasurement();
    }

    @Override
    public void stop() throws Exception {
        if (controller != null) {
            controller.stopMeasurement();
            controller.getDevice().shutdown();
        }
        super.stop();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
