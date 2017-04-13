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

import hep.dataforge.context.Context;
import hep.dataforge.fx.work.Work;
import hep.dataforge.fx.work.WorkManager;
import hep.dataforge.io.ColumnedDataWriter;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.plots.XYPlotFrame;
import hep.dataforge.plots.data.PlotDataUtils;
import hep.dataforge.plots.data.PlottableData;
import hep.dataforge.plots.data.PlottableGroup;
import hep.dataforge.plots.data.TimePlottable;
import hep.dataforge.plots.fx.FXPlotFrame;
import hep.dataforge.plots.fx.PlotContainer;
import hep.dataforge.plots.jfreechart.JFreeChartFrame;
import hep.dataforge.storage.commons.JSONMetaWriter;
import hep.dataforge.tables.*;
import inr.numass.storage.NumassData;
import inr.numass.storage.NumassPoint;
import inr.numass.utils.TritiumUtils;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.util.converter.NumberStringConverter;
import org.controlsfx.control.RangeSlider;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * FXML Controller class
 *
 * @author darksnake
 */
public class NumassLoaderViewComponent extends AnchorPane implements Initializable {

    private final Context context;

    Logger logger = LoggerFactory.getLogger(NumassLoaderViewComponent.class);
    private NumassData data;
    private PlotContainer detectorPlot;
    private PlotContainer spectrumPlot;
    private PlotContainer hvPlot;
    private PlottableData spectrumData;
    private List<NumassPoint> points;
    private ChoiceBox<Integer> detectorBinningSelector;
    private CheckBox detectorNormalizeSwitch;
    private Button detectorDataExportButton;

    @FXML
    private AnchorPane detectorPlotPane;
//    @FXML
//    private CheckListView<String> detectorPointListView;
    @FXML
    private Tab detectorTab;
//    @FXML
//    private Tab hvTab;
//    @FXML
//    private Tab spectrumTab;
    @FXML
    private TextArea infoTextBox;
    @FXML
    private AnchorPane spectrumPlotPane;
//    @FXML
//    private VBox spectrumOptionsPane;
    @FXML
    private TextField lowChannelField;
    @FXML
    private TextField upChannelField;
    @FXML
    private RangeSlider channelSlider;
//    @FXML
//    private Button spectrumExportButton;
    @FXML
    private TextField dTimeField;
    @FXML
    private AnchorPane hvPane;

    public NumassLoaderViewComponent(Context context) {
        this.context = context;
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
        //setup detector pane plot and sidebar
        Label l = new Label("Bin size:");
        l.setPadding(new Insets(5));
        detectorBinningSelector = new ChoiceBox<>(FXCollections.observableArrayList(1, 2, 5, 10, 20, 50));
        detectorBinningSelector.setMaxWidth(Double.MAX_VALUE);
        detectorBinningSelector.getSelectionModel().select(4);

        detectorNormalizeSwitch = new CheckBox("Normailize");
        detectorNormalizeSwitch.setSelected(true);
        detectorNormalizeSwitch.setPadding(new Insets(5));

        detectorPlot = PlotContainer.anchorTo(detectorPlotPane);
        detectorPlot.addToSideBar(0, l, detectorBinningSelector, detectorNormalizeSwitch, new Separator(Orientation.HORIZONTAL));

        detectorDataExportButton = new Button("Export");
        detectorDataExportButton.setMaxWidth(Double.MAX_VALUE);
        detectorDataExportButton.setOnAction(this::onExportButtonClick);
        detectorPlot.addToSideBar(detectorDataExportButton);

        detectorPlot.setSideBarPosition(0.7);
        //setup spectrum pane
        spectrumPlot = PlotContainer.anchorTo(spectrumPlotPane);

        Meta spectrumPlotMeta = new MetaBuilder("plot")
                .setValue("xAxis.axisTitle", "U")
                .setValue("xAxis.axisUnits", "V")
                .setValue("yAxis.axisTitle", "count rate")
                .setValue("yAxis.axisUnits", "Hz")
                .setValue("legend.show", false);
        spectrumPlot.setPlot(new JFreeChartFrame(spectrumPlotMeta));

        lowChannelField.textProperty().bindBidirectional(channelSlider.lowValueProperty(), new NumberStringConverter());
        upChannelField.textProperty().bindBidirectional(channelSlider.highValueProperty(), new NumberStringConverter());

        channelSlider.setHighValue(1900d);
        channelSlider.setLowValue(300d);

        ChangeListener<? super Number> rangeChangeListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            setupSpectrumPane(points);
        };

        dTimeField.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            setupSpectrumPane(points);
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

        //setup HV plot
        hvPlot = PlotContainer.anchorTo(hvPane);
        Meta hvPlotMeta = new MetaBuilder("plot")
                .setValue("xAxis.axisTitle", "time")
                .setValue("xAxis.type", "time")
                .setValue("yAxis.axisTitle", "HV");
        hvPlot.setPlot(new JFreeChartFrame(hvPlotMeta));
    }

    public NumassData getData() {
        return data;
    }

    private WorkManager getWorkManager(){
        return context.getFeature(WorkManager.class);
    }

    public void loadData(NumassData data) {
        this.data = data;
        if (data != null) {
            getWorkManager().<List<NumassPoint>>startWork("viewer.numass.load", (Work callback) -> {
                callback.setTitle("Load numass data (" + data.getName() + ")");
                points = data.getNMPoints();

                Platform.runLater(() -> {
                    //setup detector data
                    setupDetectorPane(points);
                    //setup spectrum plot
                    setupSpectrumPane(points);
                });
            });
            //setup hv plot
            Supplier<Table> hvData = data.getHVData();
            if (hvData != null) {
                setupHVPane(hvData);
            }
            setupInfo(data);

        } else {
            logger.error("The data model is null");
        }
        detectorTab.getTabPane().getSelectionModel().select(detectorTab);
    }

    private void setupHVPane(Supplier<Table> hvData) {
        getWorkManager().startWork("viewer.numass.hv", (Work callback) -> {
            Table t = hvData.get();
            Platform.runLater(() -> {
                if (t != null) {
                    hvPlot.getPlot().plottables().clear();
                    PlottableGroup<TimePlottable> set = new PlottableGroup<>();
                    for (DataPoint dp : t) {
                        String block = dp.getString("block", "default");
                        if (!set.has(block)) {
                            set.add(new TimePlottable(block, block));
                        }
                        set.get(block).put(dp.getValue("timestamp").timeValue(), dp.getValue("value"));
                    }
                    hvPlot.getPlot().addAll(set);
                }
            });
        });
    }

    /**
     * setup detector pane
     *
     * @param points
     */
    private void setupDetectorPane(List<NumassPoint> points) {
        boolean normalize = detectorNormalizeSwitch.isSelected();
        int binning = detectorBinningSelector.getValue();
        updateDetectorPane(points, binning, normalize);
        detectorBinningSelector.getSelectionModel().selectedItemProperty()
                .addListener((ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue) -> {
                    boolean norm = detectorNormalizeSwitch.isSelected();
                    updateDetectorPane(NumassLoaderViewComponent.this.points, newValue, norm);
                });
        detectorNormalizeSwitch.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            int bin = detectorBinningSelector.getValue();
            updateDetectorPane(NumassLoaderViewComponent.this.points, bin, newValue);
        });
        detectorDataExportButton.setDisable(false);
    }

    private void setupInfo(NumassData loader) {
        Meta info = loader.meta();
        infoTextBox.setText(new JSONMetaWriter().writeString(info).
                replace("\\r", "\r\t").replace("\\n", "\n\t"));
    }

    private void setupSpectrumPane(List<NumassPoint> points) {
        if (spectrumData == null) {
            spectrumData = new PlottableData("spectrum");
            spectrumPlot.getPlot().add(spectrumData);
        }

        int lowChannel = (int) channelSlider.getLowValue();
        int highChannel = (int) channelSlider.getHighValue();
        if (points == null || points.isEmpty()) {
            spectrumData.clear();
        } else {
            spectrumData.fillData(points.stream()
                    .map((NumassPoint point) -> getSpectrumPoint(point, lowChannel, highChannel, getDTime()))
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

    private DataPoint getSpectrumPoint(NumassPoint point, int lowChannel, int upChannel, double dTime) {
        double u = point.getUread();
        return new MapPoint(new String[]{XYAdapter.X_VALUE_KEY, XYAdapter.Y_VALUE_KEY, XYAdapter.Y_ERROR_KEY}, u,
                TritiumUtils.countRateWithDeadTime(point,lowChannel, upChannel, dTime),
                TritiumUtils.countRateWithDeadTimeErr(point,lowChannel, upChannel, dTime));
    }

    /**
     * update detector pane with new data
     */
    private void updateDetectorPane(List<NumassPoint> points, int binning, boolean normalize) {
        FXPlotFrame detectorPlotFrame;
        if (detectorPlot.getPlot() == null) {
            Meta frameMeta = new MetaBuilder("frame")
                    .setValue("title", "Detector response plot")
                    .setNode(new MetaBuilder("xAxis")
                            .setValue("axisTitle", "ADC")
                            .setValue("axisUnits", "channels")
                            .build())
                    .setNode(new MetaBuilder("yAxis")
                            .setValue("axisTitle", "count rate")
                            .setValue("axisUnits", "Hz")
                            .build())
                    .setNode(new MetaBuilder("legend")
                            .setValue("show", false))
                    .build();
            detectorPlotFrame = new JFreeChartFrame(frameMeta);
        } else {
            detectorPlotFrame = detectorPlot.getPlot();
            detectorPlotFrame.clear();
            detectorPlot.removePlot();
        }

        getWorkManager().startWork("viewer.numass.load.detector", (Work callback) -> {
            Meta plottableConfig = new MetaBuilder("plot")
                    .setValue("connectionType", "step")
                    .setValue("thickness", 2)
                    .setValue("showLine", true)
                    .setValue("showSymbol", false)
                    .setValue("showErrors", false)
                    .setValue("JFreeChart.cache", true)
                    .build();

            callback.setMaxProgress(points.size());
            callback.setProgress(0);
            for (NumassPoint point : points) {
                String seriesName = String.format("%d: %.2f", points.indexOf(point), point.getVoltage());
                PlottableData datum = PlottableData.plot(seriesName, new XYAdapter("chanel", "count"), point.getData(binning, normalize));
                datum.configure(plottableConfig);
                detectorPlotFrame.add(datum);
                callback.increaseProgress(1d);
                //TODO add update instead of replace action
            }
            detectorPlot.setPlot(detectorPlotFrame);
            callback.setProgressToMax();
        });

    }

    @FXML
    private void onSpectrumExportClick(ActionEvent event) {
        if (points != null && !points.isEmpty()) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choose text export destination");
            fileChooser.setInitialFileName(data.getName() + "_spectrum.onComplete");
            File destination = fileChooser.showSaveDialog(spectrumPlotPane.getScene().getWindow());
            if (destination != null) {
                String[] names = new String[]{"Uset", "Uread", "Length", "Total", "Window", "CR", "CRerr", "Timestamp"};
                int loChannel = (int) channelSlider.getLowValue();
                int upChannel = (int) channelSlider.getHighValue();
                double dTime = getDTime();
                ListTable.Builder spectrumDataSet = new ListTable.Builder(names);

                for (NumassPoint point : points) {
                    spectrumDataSet.row(new MapPoint(names, new Object[]{
                            point.getVoltage(),
                        point.getUread(),
                        point.getLength(),
                            point.getTotalCount(),
                        point.getCountInWindow(loChannel, upChannel),
                        TritiumUtils.countRateWithDeadTime(point,loChannel, upChannel, dTime),
                        TritiumUtils.countRateWithDeadTimeErr(point,loChannel, upChannel, dTime),
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
                            .writeDataSet(destination, spectrumDataSet.build(), comment, false);
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
            Table detectorData = PlotDataUtils.collectXYDataFromPlot((XYPlotFrame) detectorPlot.getPlot(), true);
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
