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
package inr.numass.control.magnet.fx;

import hep.dataforge.exceptions.PortException;
import inr.numass.control.magnet.LambdaMagnet;
import inr.numass.control.magnet.MagnetStateListener;
import inr.numass.control.magnet.MagnetStatus;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author Alexander Nozik
 */
public class MagnetControllerComponent extends AnchorPane implements Initializable, MagnetStateListener {

    private LambdaMagnet lambdaMagnet;
    private Logger logger;

    private boolean showConfirmation = true;

    public static MagnetControllerComponent build(LambdaMagnet lambdaMagnet) {
        MagnetControllerComponent component = new MagnetControllerComponent();
        FXMLLoader loader = new FXMLLoader(lambdaMagnet.getClass().getResource("/fxml/SingleMagnet.fxml"));

        loader.setRoot(component);
        loader.setController(component);

        try {
            loader.load();
        } catch (Exception ex) {
            LoggerFactory.getLogger("FX").error("Error during fxml initialization", ex);
            throw new Error(ex);
        }
        component.setLambdaMagnet(lambdaMagnet);
        return component;
    }

    @FXML
    private Label labelI;
    @FXML
    private Label labelU;
    @FXML
    private TextField targetIField;
    @FXML
    private Label magnetName;
    @FXML
    private ToggleButton monitorButton;
    @FXML
    private Label statusLabel;
    @FXML
    private ToggleButton setButton;
    @FXML
    private TextField magnetSpeedField;

//    public MagnetControllerComponent(LambdaMagnet lambdaMagnet) {
//        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SingleMagnet.fxml"));
//
//        loader.setRoot(this);
//        loader.setController(this);
//
//        try {
//            loader.load();
//        } catch (IOException ex) {
//            throw new RuntimeException(ex);
//        }
//        setLambdaMagnet(lambdaMagnet);
//    }
    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        targetIField.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            if (!newValue.matches("\\d*(\\.)?\\d*")) {
                targetIField.setText(oldValue);
            }
        });

        magnetSpeedField.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            if (!newValue.matches("\\d*(\\.)?\\d*")) {
                magnetSpeedField.setText(oldValue);
            }
        });
    }

    public void setShowConfirmation(boolean showConfirmation) {
        this.showConfirmation = showConfirmation;
    }

    @FXML
    private void onOutToggle(ActionEvent event) {
        try {
            setOutput(setButton.isSelected());
        } catch (PortException ex) {
            error(this.lambdaMagnet.getName(), null, ex);
        }
    }

    private double getTargetI() {
        return Double.parseDouble(targetIField.getText());
    }

    private void setOutput(boolean outputOn) throws PortException {
        if (outputOn) {
            if (showConfirmation) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setContentText("Изменение токов в сверхпроводящих магнитах можно производить только при выключенном напряжении на спектрометре."
                        + "\nВы уверены что напряжение выключено?");
                alert.setHeaderText("Проверьте напряжение на спектрометре!");
                alert.setHeight(150);
                alert.setTitle("Внимание!");
                alert.getButtonTypes().clear();
                alert.getButtonTypes().addAll(ButtonType.YES, ButtonType.CANCEL);

                if (alert.showAndWait().orElse(ButtonType.CANCEL).equals(ButtonType.YES)) {
                    startCurrentChange();
                } else {
                    setButton.setSelected(false);
                }
            } else {
                startCurrentChange();
            }
        } else {
            getLambdaMagnet().stopUpdateTask();
            targetIField.setDisable(false);
            magnetSpeedField.setDisable(false);
        }
    }

    private void startCurrentChange() throws PortException {
        double speed = Double.parseDouble(magnetSpeedField.getText());
        if (speed > 0 && speed <= 7) {
            lambdaMagnet.setSpeed(speed);
            magnetSpeedField.setDisable(true);
            getLambdaMagnet().setOutputMode(true);
            getLambdaMagnet().startUpdateTask(getTargetI());
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText(null);
            alert.setHeaderText("Недопустимое значение скорости изменения тока");
            alert.setTitle("Ошибка!");
            alert.show();
            setButton.setSelected(false);
            magnetSpeedField.setText(Double.toString(lambdaMagnet.getSpeed()));
        }

    }

    @FXML
    private void onMonitorToggle(ActionEvent event) {
        if (monitorButton.isSelected()) {
            getLambdaMagnet().startMonitorTask();
        } else {
            getLambdaMagnet().stopMonitorTask();
            this.labelU.setText("----");
        }
    }

    @Override
    public void error(String name, String errorMessage, Throwable throwable) {
        Platform.runLater(() -> {
            this.statusLabel.setText("ERROR");
            this.statusLabel.setTextFill(Color.RED);
        });
        this.logger.error("ERROR: {}", errorMessage, throwable);
//        MagnetStateListener.super.error(address, errorMessage, throwable); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * @return the lambdaMagnet
     */
    public LambdaMagnet getLambdaMagnet() {
        if (lambdaMagnet == null) {
            throw new RuntimeException("Magnet controller not defined");
        }
        return lambdaMagnet;
    }

    /**
     * @param lambdaMagnet the lambdaMagnet to set
     */
    private void setLambdaMagnet(LambdaMagnet lambdaMagnet) {
        this.lambdaMagnet = lambdaMagnet;
        logger = LoggerFactory.getLogger("lambda." + lambdaMagnet.getName());
        lambdaMagnet.setListener(this);
        magnetName.setText(lambdaMagnet.getName());

        magnetSpeedField.setText(Double.toString(this.lambdaMagnet.getSpeed()));

    }

    @Override
    public void acceptStatus(String name, MagnetStatus state) {
        Platform.runLater(() -> {
            this.labelI.setText(Double.toString(state.getMeasuredCurrent()));
            this.labelU.setText(Double.toString(state.getMeasuredVoltage()));
            outputModeChanged(name, state.isOutputOn());

            getLogger().info(String.format("%s (%s): Im = %f, Um = %f, Is = %f, Us = %f;",
                    name,
                    state.isOutputOn(),
                    state.getMeasuredCurrent(),
                    state.getMeasuredVoltage(),
                    state.getSetCurrent(),
                    state.getSetVoltage()
            ));

        });
    }

    @Override
    public void acceptNextI(String name, double nextI) {
        getLogger().debug("{}: nextI = {};", name, nextI);
    }

    @Override
    public void acceptMeasuredI(String name, double measuredI) {
        getLogger().debug("{}: measuredI = {};", name, measuredI);
        Platform.runLater(() -> {
            this.labelI.setText(Double.toString(measuredI));
        });
    }

    @Override
    public void outputModeChanged(String name, boolean out) {
        Platform.runLater(() -> {
            if (out) {
                this.statusLabel.setText("OK");
                this.statusLabel.setTextFill(Color.BLUE);
            } else {
                this.statusLabel.setText("OFF");
                this.statusLabel.setTextFill(Color.BLACK);
            }
        });
    }

    @Override
    public void updateTaskStateChanged(String name, boolean updateTaskRunning) {
        this.setButton.setSelected(updateTaskRunning);
        targetIField.setDisable(updateTaskRunning);
    }

    @Override
    public void monitorTaskStateChanged(String name, boolean monitorTaskRunning) {
        this.monitorButton.setScaleShape(monitorTaskRunning);
    }

    /**
     * @return the logger
     */
    public Logger getLogger() {
        return logger;
    }

//    /**
//     * @param logger the logger to set
//     */
//    public void setLogger(PrintStream logger) {
//        this.logger = logger;
//    }
    @Override
    public void displayState(String state) {
        Platform.runLater(() -> this.statusLabel.setText(state));
    }
}
