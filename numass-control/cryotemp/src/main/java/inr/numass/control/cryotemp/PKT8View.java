package inr.numass.control.cryotemp;

import hep.dataforge.context.Context;
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
import java.util.Comparator;
import java.util.ResourceBundle;

/**
 * Created by darksnake on 07-Oct-16.
 */
public class PKT8View extends DeviceViewConnection<PKT8Device> implements Initializable, MeasurementListener {

    public static PKT8ViewConnection build(Context context) {
        try {
            FXMLLoader loader = new FXMLLoader(context.getClassLoader().getResource("fxml/PKT8Indicator.fxml"));
            loader.setClassLoader(context.getClassLoader());
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
    private ToggleButton storeButton;
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
        logFragment.addRootLogHandler();
        new FragmentWindow(logFragment).bindTo(consoleButton);


        plotFragment = new PKT8PlotFragment(device);
        startStopButton.selectedProperty().setValue(getDevice().isMeasuring());

        new FragmentWindow(plotFragment).bindTo(plotButton);
        bindBooleanToState("storing", storeButton.selectedProperty());
    }

    @Override
    public void close() throws Exception {
        super.close();
        logFragment = null;
        plotFragment = null;
    }

    @Override
    public void onMeasurementResult(Measurement<?> measurement, Object result, Instant time) {
        PKT8Result res = PKT8Result.class.cast(result);
        Platform.runLater(() -> {
            lastUpdateLabel.setText(time.toString());
            table.getItems().removeIf(it -> it.getChannel().equals(res.getChannel()));
            table.getItems().add(res);
            table.getItems().sort(Comparator.comparing(PKT8Result::getChannel));
        });
    }

    @Override
    public void onMeasurementFailed(Measurement measurement, Throwable exception) {

    }


    private void startMeasurement() throws MeasurementException {
        getDevice().startMeasurement();
    }

    private void stopMeasurement() throws MeasurementException {
        if (getDevice().isMeasuring()) {
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
                getDevice().getLogger().error("Failed to start or stop device", ex);
            }
        }
    }

    @Override
    public Node getFXNode() {
        return root;
    }
}
