/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.app;

import hep.dataforge.control.measurements.Sensor;
import hep.dataforge.control.virtual.Virtual;
import hep.dataforge.meta.MetaBuilder;
import inr.numass.readvac.devices.VacCollectorDevice;
import inr.numass.readvac.fx.VacCollectorController;
import java.time.Duration;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

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

//            Sensor<Double> poweredSensor = new VirtualSensorFactory<Double>(
//                    "vac4",
//                    (sensor) -> {
//                        if (sensor.getState("power").booleanValue()) {
//                            return 1e-6;
//                        } else {
//                            return null;
//                        }
//                    })
//                    .addState("power")
//                    .setMeta(new MetaBuilder("device")
//                            .setValue("color", "magenta")
//                            .setValue("thickness", 3)
//                            .setValue("powerButton", true))
//                    .addCommand("setPower", new BiConsumer<Sensor<Double>, Value>() {
//                        @Override
//                        public void accept(Sensor<Double> sensor, Value power) {
//                            
//                        }
//                    })
//                    .build();
            VacCollectorDevice collector = new VacCollectorDevice();
            collector.setSensors(sensor1, sensor2, sensor3);
            collector.init();

//            collector.getConfig().putNode(new MetaBuilder("storage").putValue("path", "D:\\temp\\test"));

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/VacCollector.fxml"));
            loader.load();
            controller = loader.getController();
            controller.setDevice(collector);

            Scene scene = new Scene(loader.getRoot(), 800, 600);

            primaryStage.setTitle("Vacuum measurement test");
            primaryStage.setScene(scene);
            primaryStage.show();
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
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
