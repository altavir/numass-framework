package inr.numass.cryotemp;

import hep.dataforge.control.measurements.Measurement;
import hep.dataforge.control.measurements.MeasurementListener;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.exceptions.MeasurementException;
import hep.dataforge.fx.fragments.FragmentWindow;
import hep.dataforge.fx.fragments.LogFragment;
import inr.numass.control.DeviceViewConnection;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.ResourceBundle;

/**
 * Created by darksnake on 07-Oct-16.
 */
public class PKT8View extends DeviceViewConnection<PKT8Device> implements Initializable, MeasurementListener<PKT8Result> {

    public static PKT8View build(){
        try {
            FXMLLoader loader = new FXMLLoader(PKT8View.class.getResource("/fxml/PKT8Indicator.fxml"));
            loader.load();
            return loader.getController();
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private LogFragment logFragment;
    private PKT8PlotFragment plotFragment;

    @FXML
    private BorderPane root;
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
        sensorColumn.setCellValueFactory(new PropertyValueFactory<>("channel"));
        resColumn.setCellValueFactory(new PropertyValueFactory<>("rawString"));
        tempColumn.setCellValueFactory(new PropertyValueFactory<>("temperatureString"));
    }

    @Override
    public void open(@NotNull PKT8Device device) throws Exception {
        super.open(device);
        this.logFragment = new LogFragment();
        logFragment.addLogHandler(device.getContext().getLogger());
        logFragment.hookStd();//TODO to be removed later

        plotFragment = new PKT8PlotFragment(device);
        startStopButton.selectedProperty().setValue(getDevice().isMeasuring());

        new FragmentWindow(logFragment).bindTo(consoleButton);
        new FragmentWindow(plotFragment).bindTo(plotButton);
    }

    @Override
    public void close() throws Exception {
        super.close();
        logFragment = null;
        plotFragment = null;
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

    @Override
    public Node getFXNode() {
        return root;
    }
}
