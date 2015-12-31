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

import de.jensd.shichimifx.utils.ConsoleDude;
import de.jensd.shichimifx.utils.SplitPaneDividerSlider;
import hep.dataforge.exceptions.StorageException;
import inr.numass.data.NumassData;
import inr.numass.storage.NumassStorage;
import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;
import javafx.util.Pair;
import org.controlsfx.control.StatusBar;
import org.controlsfx.control.TaskProgressView;

/**
 * FXML Controller class
 *
 * @author Alexander Nozik
 */
public class MainViewerController implements Initializable, FXTaskManager {

    public static MainViewerController build(NumassStorage root) {
        MainViewerController res = new MainViewerController();
        res.setRootStorage(root);
        return res;
    }
    @FXML
    private TextArea consoleArea;
    @FXML
    private ToggleButton consoleButton;
    @FXML
    private SplitPane consoleSplit;
    @FXML
    private Button loadDirectoryButton;

    //controllers
    @FXML
    private MspViewController mspController;

    //main pane views
    @FXML
    private AnchorPane numassLoaderViewContainer;
    @FXML
    private TreeTableView<NumassLoaderTreeBuilder.TreeItemValue> numassLoaderDataTree;
    @FXML
    private StatusBar statusBar;

    //tabs
    @FXML
    private TabPane tabPane;
    @FXML
    private Tab mainTab;
    @FXML
    private Tab mspTab;
    @FXML
    private Tab pressuresTab;
    @FXML
    private Tab temperaturesTab;
    @FXML
    private Button loadRemoteButton;
    @FXML
    private Label storagePathLabel;
    @FXML
    private ScrollPane taskPane;

    private TaskProgressView progressView;

//    private Popup progressPopup;
    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
//        TabPaneDetacher.create().makeTabsDetachable(tabPane);
        ConsoleDude.hookStdStreams(consoleArea);

        SplitPaneDividerSlider slider = new SplitPaneDividerSlider(consoleSplit, 0,
                SplitPaneDividerSlider.Direction.DOWN, Duration.seconds(1));

        slider.aimContentVisibleProperty().bindBidirectional(consoleButton.selectedProperty());

        consoleButton.setSelected(false);
        loadRemoteButton.setDisable(true);

        progressView = new TaskProgressView();
        taskPane.setContent(progressView);
//        taskPane.setPrefWidth(510);
    }

    @FXML
    private void onLoadDirectory(ActionEvent event) {

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select numass storage root");
        chooser.setInitialDirectory(new File(".").getAbsoluteFile());

        final File rootDir = chooser.showDialog(((Node) event.getTarget()).getScene().getWindow());

        if (rootDir != null) {
            Task dirLoadTask = new DirectoryLoadTask(rootDir.toURI().toString());
            postTask(dirLoadTask);
            Viewer.runTask(dirLoadTask);
        }

    }

    private class DirectoryLoadTask extends Task<Void> {

        private final String uri;

        public DirectoryLoadTask(String uri) {
            this.uri = uri;
        }

        @Override
        protected Void call() throws Exception {
            updateTitle("Load storage (" + uri + ")");
            updateProgress(-1, 1);
            updateMessage("Building numass storage tree...");
            try {
                NumassStorage root = NumassStorage.buildNumassRoot(uri, true, false);
                setRootStorage(root);
                Platform.runLater(() -> storagePathLabel.setText("Storage: " + uri));
            } catch (StorageException ex) {
                updateProgress(0, 1);
                updateMessage("Failed to load storage " + uri);
                Logger.getLogger(MainViewerController.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }

    }

    @Override
    @SuppressWarnings("unchecked")
    public void postTask(Task task) {
        Platform.runLater(() -> progressView.getTasks().add(task));
    }

    public void setRootStorage(NumassStorage root) {
        Task fillTask = new StorageDataFillTask(root);
        postTask(fillTask);
        Viewer.runTask(fillTask);

        if (mspController != null) {
            mspController.setCallback(this);
            mspController.fillMspData(root);
        } else {
            mspTab.getContent().setVisible(false);
        }

        pressuresTab.getContent().setVisible(false);
        temperaturesTab.getContent().setVisible(false);

    }

    private class StorageDataFillTask extends Task<Void> {

        private final NumassStorage root;

        public StorageDataFillTask(NumassStorage root) {
            this.root = root;
        }

        @Override
        protected Void call() throws Exception {
            updateTitle("Fill data to UI (" + root.getName() + ")");
            this.updateProgress(-1, 1);
            this.updateMessage("Loading numass storage tree...");

            Task treeBuilderTask = new NumassLoaderTreeBuilder(numassLoaderDataTree, root, (NumassData loader) -> {
                NumassLoaderViewComponent component = new NumassLoaderViewComponent();
                component.loadData(loader);
                component.setCallback(MainViewerController.this);
                numassLoaderViewContainer.getChildren().clear();
                numassLoaderViewContainer.getChildren().add(component);
                AnchorPane.setTopAnchor(component, 0.0);
                AnchorPane.setRightAnchor(component, 0.0);
                AnchorPane.setLeftAnchor(component, 0.0);
                AnchorPane.setBottomAnchor(component, 0.0);
                numassLoaderViewContainer.requestLayout();
            });
            postTask(treeBuilderTask);
            Viewer.runTask(treeBuilderTask);
            try {
                treeBuilderTask.get();
                this.updateProgress(0, 1);
                this.updateMessage("Numass storage tree loaded.");
                this.succeeded();
            } catch (InterruptedException | ExecutionException ex) {
                this.failed();
                throw new RuntimeException(ex);
            }
            return null;
        }

    }

    @FXML
    private void onLoadRemote(ActionEvent event) {
        // Create the custom dialog.
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Remote storage selection");
        dialog.setHeaderText("Select remote storage login options and run");

        ButtonType loginButtonType = new ButtonType("Load", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        // Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField storageText = new TextField();
        storageText.setPrefWidth(350);
        storageText.setText("sftp://trdat:Anomaly@192.168.111.1");
        TextField runText = new TextField();
        runText.setPromptText("Run name");

        grid.add(new Label("Storage path:"), 0, 0);
        grid.add(storageText, 1, 0);
        grid.add(new Label("Run name:"), 0, 1);
        grid.add(runText, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Request focus on the username field by default.
        storageText.requestFocus();

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(storageText.getText(), runText.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        if (result.isPresent()) {
            Task dirLoadTask = new DirectoryLoadTask(result.get().getKey() + "/data/" + result.get().getValue());
            postTask(dirLoadTask);
            Viewer.runTask(dirLoadTask);
        }
    }
}
