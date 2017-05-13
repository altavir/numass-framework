package inr.numass.cryotemp;

import hep.dataforge.control.connections.DeviceConnection;
import hep.dataforge.control.devices.DeviceListener;
import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.control.measurements.MeasurementListener;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.exceptions.MeasurementException;
import hep.dataforge.fx.fragments.FragmentWindow;
import hep.dataforge.fx.fragments.LogFragment;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.time.Instant;
import java.util.ResourceBundle;

/**
 * Created by darksnake on 07-Oct-16.
 */
public class PKT8Controller extends DeviceConnection<PKT8Device> implements Initializable, DeviceListener, MeasurementListener<PKT8Result> {

    private LogFragment logFragment;
    private PKT8PlotFragment plotFragment;

    @FXML
    private ToggleButton startStopButton;
    @FXML
    private ToggleButton consoleButton;
    @FXML
    private ToggleButton plotButton;
    @FXML
    private Label lastUpdateLabel;

    @FXML
    private TableView<PKT8Result> table;
    @FXML
    private TableColumn<TableView<PKT8Result>, String> sensorColumn;
    @FXML
    private TableColumn<TableView<PKT8Result>, Double> resColumn;
    @FXML
    private TableColumn<TableView<PKT8Result>, String> tempColumn;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.logFragment = new LogFragment();
        logFragment.addLogHandler(getDevice().getContext().getLogger());
        //TODO to be removed later
        logFragment.hookStd();
        new FragmentWindow(logFragment).bindTo(consoleButton);
        plotFragment = new PKT8PlotFragment(getDevice());
        new FragmentWindow(plotFragment).bindTo(plotButton);

        sensorColumn.setCellValueFactory(new PropertyValueFactory<>("channel"));
        resColumn.setCellValueFactory(new PropertyValueFactory<>("rawString"));
        tempColumn.setCellValueFactory(new PropertyValueFactory<>("temperatureString"));
        startStopButton.selectedProperty().setValue(getDevice().isMeasuring());
    }

    @Override
    public void onMeasurementResult(Measurement<PKT8Result> measurement, PKT8Result result, Instant time) {
        Platform.runLater(() -> {
            lastUpdateLabel.setText(time.toString());
            table.getItems().removeIf(it -> it.channel.equals(result.channel));
            table.getItems().add(result);
            table.getItems().sort((o1, o2) -> o1.channel.compareTo(o2.channel));
        });
    }

    @Override
    public void onMeasurementFailed(Measurement measurement, Throwable exception) {

    }


    private void startMeasurement() throws MeasurementException {
        getDevice().startMeasurement().addListener(this);
    }

    private void stopMeasurement() throws MeasurementException {
        if (getDevice().isMeasuring()) {
            getDevice().getMeasurement().removeListener(this);
            getDevice().stopMeasurement(false);
        }
    }


    @FXML
    private void onStartStopClick(ActionEvent event) {
        if (getDevice() != null) {
            try {
                if (startStopButton.isSelected()) {
                    startMeasurement();
                } else {
                    //in case device started
                    stopMeasurement();
                }
            } catch (ControlException ex) {
                evaluateDeviceException(getDevice(), "Failed to start or stop device", ex);
            }
        }
    }
}
