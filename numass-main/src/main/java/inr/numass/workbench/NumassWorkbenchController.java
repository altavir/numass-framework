/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.workbench;

import hep.dataforge.actions.Action;
import hep.dataforge.actions.ActionManager;
import hep.dataforge.actions.ActionStateListener;
import hep.dataforge.actions.ActionUtils;
import hep.dataforge.context.Context;
import hep.dataforge.context.Global;
import hep.dataforge.data.DataNode;
import hep.dataforge.data.FileDataFactory;
import hep.dataforge.description.ActionDescriptor;
import hep.dataforge.description.DescriptorUtils;
import hep.dataforge.exceptions.NameNotFoundException;
import hep.dataforge.fx.FXDataOutputPane;
import hep.dataforge.fx.FXReportListener;
import hep.dataforge.fx.configuration.MetaEditor;
import hep.dataforge.fx.fragments.FragmentWindow;
import hep.dataforge.fx.fragments.LogFragment;
import hep.dataforge.fx.work.WorkManagerFragment;
import hep.dataforge.io.IOManager;
import hep.dataforge.io.MetaFileReader;
import hep.dataforge.meta.ConfigChangeListener;
import hep.dataforge.meta.Configuration;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.plots.PlotFrame;
import hep.dataforge.plots.PlotHolder;
import hep.dataforge.plots.PlotsPlugin;
import hep.dataforge.utils.ContextMetaFactory;
import hep.dataforge.values.Value;
import inr.numass.NumassIO;
import inr.numass.NumassProperties;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.controlsfx.control.StatusBar;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * FXML Controller class
 *
 * @author Alexander Nozik
 */
public class NumassWorkbenchController implements Initializable, StagePaneHolder, ActionStateListener, PlotHolder {

    Context parentContext;
    ContextMetaFactory<Context> contextFactory;

    List<MetaEditor> actionEditors = new ArrayList<>();
    MetaEditor dataEditor;
    Context context;

    Configuration dataConfig;
    Configuration actionsConfig;

    Map<String, StagePane> stages = new ConcurrentHashMap<>();

    WorkManagerFragment processWindow;

    FXDataOutputPane logPane;

    BooleanProperty isRunning = new SimpleBooleanProperty(false);

    @FXML
    private StatusBar statusBar;
    @FXML
    private TabPane stagesPane;
    @FXML
    private Accordion metaContainer;
    @FXML
    private Tab logTab;
    @FXML
    private ToggleButton runButton;
    @FXML
    private ToggleButton consoleButton;
    @FXML
    private ToggleButton processButton;

    @Override
    public void clearStage(String stageName) {
        StagePane sp = stages.get(stageName);
        if (sp != null) {
            sp.closeAll();
            Tab t = findTabWithName(stagesPane, stageName);
            stagesPane.getTabs().remove(t);
            stages.remove(stageName);
        }
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        logPane = new FXDataOutputPane();
        logTab.setContent(logPane.getRoot());

        LogFragment consoleWindow = new LogFragment();
        new FragmentWindow(consoleWindow).bindTo(consoleButton);
        consoleWindow.addRootLogHandler();
        consoleWindow.hookStd();

        processWindow = new WorkManagerFragment();
        new FragmentWindow(processWindow).bindTo(processButton);

        isRunning.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            runButton.setSelected(newValue);
            if (newValue) {
                runButton.setText("Stop");
            } else {
                runButton.setText("Run");
            }
        });

    }

    public Context getContext() {
        if (context == null) {
            throw new RuntimeException("Context not defined");
        } else {
            return context;
        }
    }

    /**
     * Setup context for current run
     *
     * @param config
     */
    private void buildContext(Meta config) {
        // close existing context
        if (this.context != null) {
            try {
                this.context.close();
            } catch (Exception ex) {
                context.getLogger().error("Failed to close context", ex);
            }
        }
        // building context using provided factory
        this.context = this.contextFactory.build(parentContext, config);

        // attachig visual process manager
        processWindow.setManager(context.taskManager());

        // setting io manager
        context.setIO(new WorkbenchIOManager(new NumassIO(), this));
        buildContextPane();
        context.getLog().addListener(new FXReportListener(logPane));

        // display plots iside workbench
        PlotsPlugin.buildFrom(context).setPlotHolder(this);
    }

    private Tab findTabWithName(TabPane pane, String name) {
        return pane.getTabs().stream().filter((t) -> t.getText().equals(name)).findFirst().orElse(null);
    }

    /**
     * build or get tabPane for a given stage
     *
     * @param stage
     * @return
     */
    @Override
    public synchronized StagePane getStagePane(String stage) {
        if (!stages.containsKey(stage)) {
            Tab stageTab = new Tab(stage);
            StagePane stageTabPane = new StagePane();
            stageTab.setContent(stageTabPane);
            stages.put(stage, stageTabPane);
            Platform.runLater(() -> stagesPane.getTabs().add(stageTab));
            return stageTabPane;
        } else {
            return stages.get(stage);
        }
    }

    private List<ActionDescriptor> listActions() {
        return ActionManager.buildFrom(getContext()).listActions();
    }

    private ActionDescriptor getDescriptorForAction(String actionType) {
        return listActions().stream().filter((a) -> a.getName().equals(actionType)).findFirst().orElse(null);
    }

    private void buildContextPane() {
        Configuration contextValues = new Configuration("context");
        //TODO add asMeta method to Context and replace map here
        context.getProperties().entrySet().stream().forEach((item) -> {
            contextValues.setValue(item.getKey(), item.getValue());
        });

        contextValues.addObserver(new ConfigChangeListener() {
            @Override
            public void notifyValueChanged(String name, Value oldItem, Value newItem) {
                context.putValue(name, newItem);
            }

            @Override
            public void notifyElementChanged(String name, List<? extends Meta> oldItem, List<? extends Meta> newItem) {

            }
        });

        MetaEditor contextEditor = MetaEditor.build(contextValues, null);

        contextEditor.geTable().setShowRoot(false);
        TitledPane contextPane = new TitledPane("Context", contextEditor);
        metaContainer.getPanes().add(contextPane);
    }

    public void loadConfig(Meta config) {
        cleanUp();
        buildContext(config);

        //loading data configuration
        if (config.hasMeta("data")) {
            dataConfig = new Configuration(config.getMeta("data"));
            //replacing file name value with appropriate nodes
            if (dataConfig.hasValue("file")) {
                Value fileValue = dataConfig.getValue("file");
                dataConfig.removeValue("file");
                fileValue.listValue().stream().forEach((fileName) -> {
                    dataConfig.putNode(new MetaBuilder("file")
                            .putValue("path", fileName));
                });
            }
            dataEditor = MetaEditor.build(dataConfig,
                    DescriptorUtils.buildDescriptor(
                            DescriptorUtils.findAnnotatedElement("class::hep.dataforge.data.FileDataFactory")
                    ));
            dataEditor.geTable().setShowRoot(false);
            metaContainer.getPanes().add(new TitledPane("Data", dataEditor));
        }

        //loading actions configuration
        actionsConfig = new Configuration("actionlist");

        List<Configuration> actions = config.getMetaList("action").stream()
                .map(m -> new Configuration(m)).collect(Collectors.toList());

        actionsConfig.attachNodeItem("action", actions);

        int counter = 0;
        for (Configuration action : actions) {
            counter++;
            MetaEditor actionEditor = new MetaEditor();
            //Freezing actions names
//            rootItem.setFrozenValuePredicate((c, n) -> c.getName().equals("action") && n.equals("type"));
            actionEditor.setRoot(action, getDescriptorForAction(action.getString("type")));

            actionEditors.add(actionEditor);
            String actionTitle = String.format("action [%d]: %s", counter, action.getString("type"));
            TitledPane actionPane = new TitledPane(actionTitle, actionEditor);
            metaContainer.getPanes().add(actionPane);
        }
        runButton.setDisable(false);
    }

    private void clearAllStages() {
        logPane.clear();
        stages.keySet().stream().forEach((stageName) -> {
            clearStage(stageName);
        });
    }

    /**
     * Clean up results and configuration panes
     */
    private synchronized void cleanUp() {
        //clear previus action panes
        if (processWindow.getManager() != null) {
            processWindow.getManager().cleanup();
        }
        metaContainer.getPanes().clear();
        clearAllStages();
        actionsConfig = null;
        dataConfig = null;
    }

    @FXML
    private void onLoadConfigClick(ActionEvent event) {
        statusBar.setText("Loading configuration file...");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Configuration File");
        String storageRoot = NumassProperties.getNumassProperty("numass.workbench.root");
        if (storageRoot == null || !new File(storageRoot).exists()) {
            fileChooser.setInitialDirectory(new File(".").getAbsoluteFile());
        } else {
            fileChooser.setInitialDirectory(new File(storageRoot));
        }
        File cfgFile = fileChooser.showOpenDialog(((Node) event.getTarget()).getScene().getWindow());
        if (cfgFile != null) {
            NumassProperties.setNumassProperty("numass.workbench.root", cfgFile.getParentFile().getAbsolutePath());
            try {
                Meta cfg = MetaFileReader.read(cfgFile).build();
                this.loadConfig(cfg);
                getContext().putValue(IOManager.ROOT_DIRECTORY_CONTEXT_KEY, cfgFile.getParentFile().toString());

            } catch (IOException | ParseException ex) {
                context.getLogger().error("Error reading configuration file", ex);
            }
            statusBar.setText("Configuration file loaded");
        } else {
            statusBar.setText("Loading configuration file canceled");
        }

    }

    @FXML
    private void onRunButtonClick(ActionEvent event) {
        if (!isRunning.get()) {
            if (context != null && !actionsConfig.isEmpty()) {
                statusBar.setText("Starting action execution");
                runActions();
            }
        } else {
            this.context.taskManager().shutdown();
        }
    }

    public Meta getDataConfiguration() {
        return dataConfig == null ? Meta.empty() : new MetaBuilder(dataConfig).substituteValues(getContext()).build();
    }

    public Meta getActionConfiguration() {
        return actionsConfig == null ? Meta.empty() : new MetaBuilder(actionsConfig).substituteValues(getContext()).build();
    }

    @SuppressWarnings("unchecked")
    public void runActions() {
        clearAllStages();
//        processWindow.show();
        new Thread(() -> {

            Platform.runLater(() -> {
                isRunning.set(true);
                statusBar.setProgress(-1);
            });

            DataNode data = new FileDataFactory().build(getContext(), getDataConfiguration());
            try {
                ActionUtils.runAction(getContext(), data, getActionConfiguration()).computeAll();
                Platform.runLater(() -> statusBar.setText("Execution complete"));
            } catch (Exception ex) {
                if (ex instanceof java.util.concurrent.CancellationException) {
                    //cach cancelation exception
                    Global.instance().getLogger().info("Interrupted by user");
                    Platform.runLater(() -> {
                        statusBar.setText("Execution interrupted by user");
                    });
                } else {
                    //cach all other exceptions
                    Global.instance().getLogger().error("Exception while executing action chain", ex);
                    ex.printStackTrace(System.err);
                    Platform.runLater(() -> {
                        //printing stack trace to the default output
                        statusBar.setText("Execution failed");
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Exception!");
                        alert.setHeaderText("Action execution failure");
                        alert.setContentText(ex.getMessage());
                        alert.show();
                    });
                }
            } finally {
                Platform.runLater(() -> {
                    isRunning.set(false);
                    statusBar.setProgress(0);
                });
            }

        }, "actions").start();
    }

    public void setParentContext(Context parentContext) {
        this.parentContext = parentContext;
    }

    public void setContextFactory(ContextMetaFactory<Context> contextFactory) {
        this.contextFactory = contextFactory;
    }

    @Override
    public void notifyActionStarted(Action action, Object argument) {
        Platform.runLater(() -> statusBar.setText(String.format("Action '%s' started", action.getName())));
    }

    @Override
    public void notifyActionFinished(Action action, Object result) {
        Platform.runLater(() -> statusBar.setText(String.format("Action '%s' fineshed", action.getName())));
    }

    @Override
    public void notifyAcionProgress(Action action, double progress, String message) {
        Platform.runLater(() -> {
            statusBar.setText(String.format("%s: %s started", action.getName(), message));
            if (progress > 0) {
                statusBar.setProgress(progress);
            }
        });
    }

    @Override
    public PlotFrame buildPlotFrame(String stage, String name, Meta annotation) {
        return getStagePane(stage).buildPlotOutput(name, annotation);
    }

    @Override
    public PlotFrame getPlotFrame(String stage, String name) throws NameNotFoundException {
        StagePane pane = getStagePane(stage);
        Tab tab = findTabWithName(pane, "image::" + name);
        if (tab != null && tab instanceof PlotOutputTab) {
            return ((PlotOutputTab) tab).getFrame();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasPlotFrame(String stage, String name) {
        StagePane pane = getStagePane(stage);
        Tab tab = findTabWithName(pane, "image::" + name);
        return (tab != null && tab instanceof PlotOutputTab);
    }

}
