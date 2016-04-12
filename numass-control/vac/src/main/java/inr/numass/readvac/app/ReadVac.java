/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.app;

import hep.dataforge.control.measurements.Sensor;
import hep.dataforge.io.MetaFileReader;
import hep.dataforge.meta.Meta;
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

/**
 *
 * @author Alexander Nozik
 */
public class ReadVac extends Application {

    VacCollectorController controller;

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

        Sensor<Double> p1 = new MKSVacDevice(config.getString("p1.port","com::/dev/ttyUSB0"));
        p1.configure(config.getNode("p1",Meta.empty()));
        p1.setName(config.getString("p1.name","P1"));
        p1.getConfig().putValue("powerButton", true);
        Sensor<Double> p2 = new CM32Device(config.getString("p2.port","tcp::192.168.111.32:4002"));
        p1.configure(config.getNode("p2",Meta.empty()));
        p2.setName(config.getString("p2.name","P2"));
        Sensor<Double> p3 = new CM32Device(config.getString("p3.port","tcp::192.168.111.32:4003"));
        p1.configure(config.getNode("p3",Meta.empty()));
        p3.setName(config.getString("p3.name","P3"));
        Sensor<Double> px = new VITVacDevice(config.getString("px.port","com::/dev/ttyUSB1"));
        p1.configure(config.getNode("px",Meta.empty()));
        px.setName(config.getString("px.name","Px"));
        Sensor<Double> baratron = new MKSBaratronDevice(config.getString("baratron.port","tcp::192.168.111.33:4004"));
        baratron.setName(config.getString("baratron.name","Baratron"));
        p1.configure(config.getNode("baratron",Meta.empty()));

        VacCollectorDevice collector = new VacCollectorDevice();
        collector.setSensors(p1, p2, p3, px, baratron);
        collector.init();
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/VacCollector.fxml"));
        loader.load();
        controller = loader.getController();
        controller.setDevice(collector);

        Scene scene = new Scene(loader.getRoot(), 800, 600);

        primaryStage.setTitle("Numass vacuum measurements");
        primaryStage.setScene(scene);
        primaryStage.show();
        controller.startMeasurement();
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
