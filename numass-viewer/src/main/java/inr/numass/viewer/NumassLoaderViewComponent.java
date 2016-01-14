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
package inr.numass.viewer;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import hep.dataforge.data.DataPoint;
import hep.dataforge.data.DataSet;
import hep.dataforge.data.ListDataSet;
import hep.dataforge.data.MapDataPoint;
import hep.dataforge.data.XYDataAdapter;
import hep.dataforge.io.ColumnedDataWriter;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.plots.XYPlotFrame;
import hep.dataforge.plots.XYPlottable;
import hep.dataforge.plots.data.ChangeablePlottableData;
import hep.dataforge.plots.data.PlotDataUtils;
import hep.dataforge.plots.data.PlottableData;
import hep.dataforge.plots.jfreechart.JFreeChartFrame;
import hep.dataforge.storage.commons.JSONMetaWriter;
import inr.numass.data.NMPoint;
import inr.numass.data.NumassData;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.converter.NumberStringConverter;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.RangeSlider;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FXML Controller class
 *
 * @author darksnake
 */
public class NumassLoaderViewComponent extends AnchorPane implements Initializable {

    private FXTaskManager callback;

    Logger logger = LoggerFactory.getLogger(NumassLoaderViewComponent.class);
    private NumassData data;
    private XYPlotFrame detectorPlotFrame;
    private XYPlotFrame spectrumPlotFrame;
    private ChangeablePlottableData spectrumData;
    private List<NMPoint> points;

    @FXML
    private AnchorPane detectorPlotPane;
    @FXML
    private CheckListView<String> detectorPointListView;
    @FXML
    private Tab detectorTab;
    @FXML
    private Tab hvTab;
    @FXML
    private Tab spectrumTab;
    @FXML
    private TextArea infoTextBox;
    @FXML
    private VBox detectorOptionsPane;
    @FXML
    private AnchorPane spectrumPlotPane;
    @FXML
    private VBox spectrumOptionsPane;
    @FXML
    private ChoiceBox<Integer> detectorBinningSelector;
    @FXML
    private CheckBox detectorNormalizeSwitch;
    @FXML
    private Button detectorDataExportButton;
    @FXML
    private TextField lowChannelField;
    @FXML
    private TextField upChannelField;
    @FXML
    private RangeSlider channelSlider;
    @FXML
    private Button spectrumExportButton;
    @FXML
    private TextField dTimeField;

    public NumassLoaderViewComponent() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NumassLoaderView.fxml"));

        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        detectorBinningSelector.setItems(FXCollections.observableArrayList(1, 2, 5, 10, 20, 50));
        detectorBinningSelector.getSelectionModel().select(4);
        detectorNormalizeSwitch.setSelected(true);

        detectorPointListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        detectorDataExportButton.setOnAction(this::onExportButtonClick);
        lowChannelField.textProperty().bindBidirectional(channelSlider.lowValueProperty(), new NumberStringConverter());
        upChannelField.textProperty().bindBidirectional(channelSlider.highValueProperty(), new NumberStringConverter());

        channelSlider.setHighValue(1900d);
        channelSlider.setLowValue(300d);

        ChangeListener<? super Number> rangeChangeListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            updateSpectrumPane(points);
        };

        dTimeField.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            updateSpectrumPane(points);
        });

        channelSlider.lowValueProperty().addListener(rangeChangeListener);
        channelSlider.highValueProperty().addListener(rangeChangeListener);

        ValidationSupport validationSupport = new ValidationSupport();
        Predicate<String> isNumber = (String t) -> {
            try {
                Double.parseDouble(t);
                return true;
            } catch (NumberFormatException | NullPointerException ex) {
                return false;
            }
        };

        validationSupport.registerValidator(dTimeField, Validator.createPredicateValidator(isNumber, "Must be number"));
    }

    public NumassData getData() {
        return data;
    }

    public void setCallback(FXTaskManager callback) {
        this.callback = callback;
    }

    public void loadData(NumassData data) {
        this.data = data;
        if (data != null) {
            LoadPointsTask task = new LoadPointsTask(data);
            if (callback != null) {
                callback.postTask(task);
            }
            Viewer.runTask(task);
            try {
                this.points = task.get();
            } catch (InterruptedException |ExecutionException ex) {
                logger.error("Can't load spectrum data points", ex);
            }
        } else {
            logger.error("The data model is null");
        }
        detectorTab.getTabPane().getSelectionModel().select(detectorTab);
    }

    private class LoadPointsTask extends Task<List<NMPoint>> {

        private final NumassData loader;

        public LoadPointsTask(NumassData loader) {
            this.loader = loader;
        }

        @Override
        protected List<NMPoint> call() throws Exception {
            updateTitle("Load numass data (" + loader.getName() + ")");
            List<NMPoint> points = loader.getNMPoints();
            Platform.runLater(() -> {
                //setup detector data
                setupDetectorPane(points);
                //setup spectrum plot
                updateSpectrumPane(points);

                setupInfo(data);
            });
            return points;
        }

    }

    /**
     * setup detector pane
     *
     * @param points
     */
    private void setupDetectorPane(List<NMPoint> points) {
        boolean normalize = detectorNormalizeSwitch.isSelected();
        int binning = detectorBinningSelector.getValue();
        updateDetectorPane(fillDetectorData(points, binning, normalize));
        detectorBinningSelector.getSelectionModel().selectedItemProperty()
                .addListener((ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue) -> {
                    boolean norm = detectorNormalizeSwitch.isSelected();
                    updateDetectorPane(fillDetectorData(NumassLoaderViewComponent.this.points, newValue, norm));
                });
        detectorNormalizeSwitch.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            int bin = detectorBinningSelector.getValue();
            updateDetectorPane(fillDetectorData(NumassLoaderViewComponent.this.points, bin, newValue));
        });
        detectorDataExportButton.setDisable(false);
    }

    private void setupInfo(NumassData loader) {
        Meta info = loader.getInfo();
        infoTextBox.setText(new JSONMetaWriter().writeString(info, null).
                replace("\\r", "\r\t").replace("\\n", "\n\t"));
    }

    private void updateSpectrumPane(List<NMPoint> points) {
        if (spectrumPlotFrame == null) {
            Meta plotMeta = new MetaBuilder("plot")
                    .setValue("xAxis.axisTitle", "U")
                    .setValue("xAxis.axisUnits", "V")
                    .setValue("yAxis.axisTitle", "count rate")
                    .setValue("yAxis.axisUnits", "Hz")
                    .setValue("legend.show", false);

            spectrumPlotFrame = new JFreeChartFrame("spectrum", plotMeta).display(spectrumPlotPane);
        }

        if (spectrumData == null) {
            spectrumData = new ChangeablePlottableData("spectrum", null);
            spectrumPlotFrame.add(spectrumData);
        }

        int lowChannel = (int) channelSlider.getLowValue();
        int highChannel = (int) channelSlider.getHighValue();
        if (points == null || points.isEmpty()) {
            spectrumData.clear();
        } else {
            spectrumData.fillData(points.stream()
                    .<DataPoint>map((NMPoint point) -> getSpectrumPoint(point, lowChannel, highChannel, getDTime()))
                    .collect(Collectors.toList()));
        }
    }

    private double getDTime() {
        try {
            return Double.parseDouble(dTimeField.getText()) * 1e-6;
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private DataPoint getSpectrumPoint(NMPoint point, int lowChannel, int upChannel, double dTime) {
        double u = point.getUread();
        return new MapDataPoint(new String[]{"x", "y", "yErr"}, u,
                point.getCountRate(lowChannel, upChannel, dTime),
                point.getCountRateErr(lowChannel, upChannel, dTime));
    }

    /**
     * update detector pane with new data
     */
    private void updateDetectorPane(List<XYPlottable> detectorData) {
        Platform.runLater(() -> {
            if (detectorData == null) {
                throw new IllegalArgumentException("Detector data not defined");
            }

            detectorPointListView.getItems().clear();//removing all checkboxes
            detectorPlotPane.getChildren().clear();//removing plot 

            Meta frameMeta = new MetaBuilder("frame")
                    .setValue("frameTitle", "Detector response plot")
                    .setNode(new MetaBuilder("xAxis")
                            .setValue("axisTitle", "ADC")
                            .setValue("axisUnits", "channels")
                            .build())
                    .setNode(new MetaBuilder("yAxis")
                            .setValue("axisTitle", "count rate")
                            .setValue("axisUnits", "Hz")
                            .build())
                    .build();

            detectorPlotFrame = new JFreeChartFrame("detectorSignal", frameMeta).display(detectorPlotPane);

            for (XYPlottable pl : detectorData) {
                detectorPlotFrame.add(pl);
                detectorPointListView.getItems().add(pl.getName());
            }

            for (String plotName : detectorPointListView.getItems()) {
                BooleanProperty checked = detectorPointListView.getItemBooleanProperty(plotName);
                checked.set(true);

                checked.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                    detectorPlotFrame.get(plotName).getConfig().setValue("visible", newValue);
                });
            }
        });
    }

    private List<XYPlottable> fillDetectorData(List<NMPoint> points, int binning, boolean normalize) {
        List<XYPlottable> plottables = new ArrayList<>();
        Meta plottableConfig = new MetaBuilder("plot")
                .setValue("connectionType", "step")
                .setValue("thickness", 2)
                .setValue("showLine", true)
                .setValue("showSymbol", false)
                .setValue("showErrors", false)
                .build();

        for (NMPoint point : points) {
            String seriesName = String.format("%d: %.2f (%.2f)", points.indexOf(point), point.getUset(), point.getUread());

            PlottableData datum = PlottableData.plot(seriesName,new XYDataAdapter("chanel", "count"), point.getData(binning, normalize));
            datum.configure(plottableConfig);
            plottables.add(datum);
        }
        return plottables;
    }

    @FXML
    private void checkAllAction(ActionEvent event) {
        detectorPointListView.getCheckModel().checkAll();
    }

    @FXML
    private void uncheckAllAction(ActionEvent event) {
        detectorPointListView.getCheckModel().clearChecks();
    }

    @FXML
    private void checkSelectedAction(ActionEvent event) {
        for (Integer i : detectorPointListView.getSelectionModel().getSelectedIndices()) {
            detectorPointListView.getCheckModel().check(i);
        }
    }

    @FXML
    private void uncheckSelectedAction(ActionEvent event) {
        for (Integer i : detectorPointListView.getSelectionModel().getSelectedIndices()) {
            detectorPointListView.getCheckModel().clearCheck(i);
        }
    }

    @FXML
    private void onSpectrumExportClick(ActionEvent event) {
        if (points != null && !points.isEmpty()) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choose text export destination");
            fileChooser.setInitialFileName(data.getName() + "_spectrum.out");
            File destination = fileChooser.showSaveDialog(spectrumPlotPane.getScene().getWindow());
            if (destination != null) {
                String[] names = new String[]{"Uset", "Uread", "Length", "Total", "Window", "CR", "CRerr", "Timestamp"};
                int loChannel = (int) channelSlider.getLowValue();
                int upChannel = (int) channelSlider.getHighValue();
                double dTime = getDTime();
                ListDataSet spectrumDataSet = new ListDataSet(names);

                for (NMPoint point : points) {
                    spectrumDataSet.add(new MapDataPoint(names, new Object[]{
                        point.getUset(),
                        point.getUread(),
                        point.getLength(),
                        point.getEventsCount(),
                        point.getCountInWindow(loChannel, upChannel),
                        point.getCountRate(loChannel, upChannel, dTime),
                        point.getCountRateErr(loChannel, upChannel, dTime),
                        point.getStartTime()
                    }
                    ));
                }

                try {
                    String comment = String.format("Numass data viewer spectrum data export for %s%n"
                            + "Window: (%d, %d)%n"
                            + "Dead time per event: %g%n",
                            data.getName(), loChannel, upChannel, dTime);

                    ColumnedDataWriter
                            .writeDataSet(destination, spectrumDataSet, comment, false);
                } catch (IOException ex) {
                    LoggerFactory.getLogger(getClass()).error("Destination file not found", ex);
                }
            }
        }
    }

    private void onExportButtonClick(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose text export destination");
        fileChooser.setInitialFileName(data.getName() + "_detector.out");
        File destination = fileChooser.showSaveDialog(detectorPlotPane.getScene().getWindow());
        if (destination != null) {
            DataSet detectorData = PlotDataUtils.collectXYDataFromPlot(detectorPlotFrame, true);
            try {
                ColumnedDataWriter
                        .writeDataSet(destination, detectorData, "Numass data viewer detector data export for " + data.getName(),
                                false);
            } catch (IOException ex) {
                LoggerFactory.getLogger(getClass()).error("Destination file not found", ex);
            }
        }

    }

}
