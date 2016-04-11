/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.app;

import hep.dataforge.control.measurements.Sensor;
import inr.numass.readvac.devices.CM32Device;
import inr.numass.readvac.devices.MKSBaratronDevice;
import inr.numass.readvac.devices.MKSVacDevice;
import inr.numass.readvac.devices.VITVacDevice;
import inr.numass.readvac.devices.VacCollectorDevice;
import inr.numass.readvac.fx.VacCollectorController;
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
        Sensor<Double> p1 = new MKSVacDevice("com::/dev/ttyUSB0");
        p1.setName("P1");
        p1.getConfig().putValue("powerButton", true);
        Sensor<Double> p2 = new CM32Device("tcp::192.168.111.32:4002");
        p2.setName("P2");
        Sensor<Double> p3 = new CM32Device("tcp::192.168.111.32:4003");
        p3.setName("P3");
        Sensor<Double> px = new VITVacDevice("com::/dev/ttyUSB1");
        px.setName("Px");
        Sensor<Double> baratron = new MKSBaratronDevice("tcp::192.168.111.33:4004");
        baratron.setName("Baratron");

        VacCollectorDevice collector = new VacCollectorDevice();
        collector.setSensors(p1, p2, p3, px, baratron);
//            collector.setSensors(baratron);
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
