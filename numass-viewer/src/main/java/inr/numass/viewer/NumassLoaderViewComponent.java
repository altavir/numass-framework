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
import hep.dataforge.data.MapDataPoint;
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
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FXML Controller class
 *
 * @author darksnake
 */
public class NumassLoaderViewComponent extends AnchorPane implements Initializable {

    public static NumassLoaderViewComponent build(NumassData numassLoader) {
        NumassLoaderViewComponent component = new NumassLoaderViewComponent();
        FXMLLoader loader = new FXMLLoader(component.getClass().getResource("/fxml/NumassLoaderView.fxml"));

        loader.setRoot(component);
        loader.setController(component);

        try {
            loader.load();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        component.setData(numassLoader);
        return component;
    }

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

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        detectorBinningSelector.setItems(FXCollections.observableArrayList(1, 2, 5, 10, 20));
        detectorBinningSelector.getSelectionModel().selectLast();
        detectorNormalizeSwitch.setSelected(true);

        detectorPointListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        detectorDataExportButton.setOnAction(this::onExportButtonClick);
        lowChannelField.textProperty().bindBidirectional(channelSlider.lowValueProperty(), new NumberStringConverter());
        upChannelField.textProperty().bindBidirectional(channelSlider.highValueProperty(), new NumberStringConverter());

        channelSlider.setLowValue(300);
        channelSlider.setHighValue(1900);

        ChangeListener<? super Number> rangeChangeListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            updateSpectrumPane();
        };

        channelSlider.lowValueProperty().addListener(rangeChangeListener);
        channelSlider.highValueProperty().addListener(rangeChangeListener);
    }

    public NumassData getData() {
        return data;
    }

    public void setData(NumassData data) {
        this.data = data;
        if (data != null) {
            points = data.getNMPoints();
            //setup detector data
            setupDetectorPane(points);
            //setup spectrum plot
            updateSpectrumPane();

            setupInfo(data);

            detectorTab.getTabPane().getSelectionModel().select(detectorTab);
        } else {
            logger.error("The data model is null");
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
                    updateDetectorPane(fillDetectorData(points, newValue, norm));
                });
        detectorNormalizeSwitch.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            int bin = detectorBinningSelector.getValue();
            updateDetectorPane(fillDetectorData(points, bin, newValue));
        });
        detectorDataExportButton.setDisable(false);
    }

    private void setupInfo(NumassData loader) {
        Meta info = loader.getInfo();
        infoTextBox.setText(new JSONMetaWriter().writeString(info, null).
                replace("\\r", "\r").replace("\\n", "\n"));
    }

    private void updateSpectrumPane() {
        if (spectrumPlotFrame == null) {
            Meta plotMeta = new MetaBuilder("plot")
                    .setValue("xAxis.axisTitle", "U")
                    .setValue("xAxis.axisUnits", "V")
                    .setValue("yAxis.axisTitle", "count rate")
                    .setValue("yAxis.axisUnits", "Hz")
                    .setValue("legend.show", false);

            spectrumPlotFrame = new JFreeChartFrame("spectrum", plotMeta, spectrumPlotPane);
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
                    .<DataPoint>map((NMPoint point) -> getSpectrumPoint(point, lowChannel, highChannel))
                    .collect(Collectors.toList()));
        }
    }

    private DataPoint getSpectrumPoint(NMPoint point, int lowChannel, int highChannel) {
        double u = point.getUread();
        double count = point.getCountInWindow(lowChannel, highChannel);
        double time = point.getLength();
        double err = Math.sqrt(count);
        return new MapDataPoint(new String[]{"x", "y", "yErr"}, u, count / time, err / time);
    }

//    private void setupSpectrumPane(List<NMPoint> points, int lowChannel, int upChannel) {
//        updateSpectrumData(fillSpectrumData(points, (point) -> point.getCountInWindow(lowChannel, upChannel)));
//    }
//
//    private void updateSpectrumData(XYIntervalSeriesCollection data) {
//        spectrumPlotPane.getChildren().clear();
//        NumberAxis xAxis = new NumberAxis("HV");
//        NumberAxis yAxis = new NumberAxis("count rate");
//
//        xAxis.setAutoRangeIncludesZero(false);
//        yAxis.setAutoRangeIncludesZero(false);
//
//        XYPlot plot = new XYPlot(data, xAxis, yAxis, new XYErrorRenderer());
//        JFreeChart spectrumPlot = new JFreeChart("spectrum", plot);
//        displayPlot(spectrumPlotPane, spectrumPlot);
//    }
    /**
     * update detector pane with new data
     */
    private void updateDetectorPane(List<XYPlottable> detectorData) {
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

        detectorPlotFrame = new JFreeChartFrame("detectorSignal", frameMeta, detectorPlotPane);

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
    }

    private List<XYPlottable> fillDetectorData(List<NMPoint> points, int binning, boolean normalize) {
        List<XYPlottable> plottables = new ArrayList<>();
        Meta plottableConfig = new MetaBuilder("plot")
                .setValue("connectionType", "step")
                .setValue("thickness", 2)
                .setValue("showLine", true)
                .setValue("showSymbol", false)
                .build();

        for (NMPoint point : points) {
            String seriesName = String.format("%d: %.2f (%.2f)", points.indexOf(point), point.getUset(), point.getUread());

            PlottableData datum = new PlottableData(seriesName, plottableConfig, point.getData(binning, normalize), "chanel", "count");
            plottables.add(datum);
        }
        return plottables;
    }

//    /**
//     * Fill spectrum with custom window calculator
//     *
//     * @param points
//     * @param lowerBoundCalculator
//     * @param upperBoundCalculator
//     * @return
//     */
//    private XYIntervalSeriesCollection fillSpectrumData(List<NMPoint> points, Function<NMPoint, Number> calculator) {
//        XYIntervalSeriesCollection collection = new XYIntervalSeriesCollection();
//        XYIntervalSeries ser = new XYIntervalSeries("spectrum");
//        for (NMPoint point : points) {
//            double u = point.getUread();
//            double count = calculator.apply(point).doubleValue();
//            double time = point.getLength();
//            double err = Math.sqrt(count);
//            ser.add(u, u, u, count / time, (count - err) / time, (count + err) / time);
//        }
//        collection.addSeries(ser);
//        return collection;
//    }
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
