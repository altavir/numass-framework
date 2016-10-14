package inr.numass.cryotemp;

import hep.dataforge.control.devices.Device;
import hep.dataforge.control.devices.DeviceListener;
import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.control.measurements.MeasurementListener;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.exceptions.MeasurementException;
import hep.dataforge.fx.fragments.ConsoleFragment;
import hep.dataforge.fx.fragments.FragmentWindow;
import hep.dataforge.values.Value;
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
public class PKT8Controller implements Initializable, DeviceListener, MeasurementListener<PKT8Result> {

    private final PKT8Device device;
    private ConsoleFragment consoleFragment;
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


    public PKT8Controller(PKT8Device device) {
        this.device = device;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.consoleFragment = new ConsoleFragment();
        consoleFragment.addLogHandler(device.getContext().getLogger());
        //TODO to be removed later
        consoleFragment.hookStd();
        new FragmentWindow(consoleFragment).bindTo(consoleButton);
        plotFragment = new PKT8PlotFragment(device);
        new FragmentWindow(plotFragment).bindTo(plotButton);

        sensorColumn.setCellValueFactory(new PropertyValueFactory<>("channel"));
        resColumn.setCellValueFactory(new PropertyValueFactory<>("rawString"));
        tempColumn.setCellValueFactory(new PropertyValueFactory<>("temperatureString"));
        startStopButton.selectedProperty().setValue(device.isMeasuring());
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

    @Override
    public void notifyDeviceStateChanged(Device device, String name, Value state) {

    }

    @Override
    public void evaluateDeviceException(Device device, String message, Throwable exception) {

    }


    public void start() throws MeasurementException {
        device.startMeasurement().addListener(this);
    }

    public void stop() throws MeasurementException {
        if (device.isMeasuring()) {
            device.getMeasurement().removeListener(this);
            device.stopMeasurement(false);
        }
    }


    @FXML
    private void onStartStopClick(ActionEvent event) {
        if (device != null) {
            try {
                if (startStopButton.isSelected()) {
                    start();
                } else {
                    //in case device started
                    stop();
                }
            } catch (ControlException ex) {
                evaluateDeviceException(device, "Failed to start or stop device", ex);
            }
        }
    }
}
