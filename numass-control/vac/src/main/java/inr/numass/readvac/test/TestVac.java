/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.test;

import hep.dataforge.context.GlobalContext;
import hep.dataforge.control.measurements.Sensor;
import hep.dataforge.control.virtual.Virtual;
import hep.dataforge.exceptions.ControlException;
import inr.numass.readvac.devices.VacCollectorDevice;
import inr.numass.readvac.fx.VacCollectorController;
import java.io.IOException;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Alexander Nozik
 */
public class TestVac extends Application {

    VacCollectorController controller;

    @Override
    public void start(Stage primaryStage) {
        try {
            Sensor<Double> sensor1 = Virtual.randomDoubleSensor("vac1", Duration.ofMillis(200), 1e-5, 2e-6);
            Sensor<Double> sensor2 = Virtual.randomDoubleSensor("vac2", Duration.ofMillis(200), 2e-5, 2e-6);
            Sensor<Double> sensor3 = Virtual.randomDoubleSensor("vac3", Duration.ofMillis(200), 1e-7, 1e-8);

            VacCollectorDevice collector = new VacCollectorDevice("collector", GlobalContext.instance(), null, sensor3, sensor2, sensor1);
            collector.init();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/VacCollector.fxml"));
            loader.load();
            controller = loader.getController();
            controller.setDevice(collector);

            Scene scene = new Scene(loader.getRoot(), 800, 600);

            primaryStage.setTitle("Vacuum measurement test");
            primaryStage.setScene(scene);
            primaryStage.show();
            controller.startMeasurement();
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    @Override
    public void stop() throws Exception {
        if (controller != null) {
            controller.stopMeasurement();
            controller.getDevice().shutdown();
        }
        super.stop();
        System.exit(0);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
