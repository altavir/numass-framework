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
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
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
import javafx.util.Pair;
import org.controlsfx.control.StatusBar;

/**
 * FXML Controller class
 *
 * @author Alexander Nozik
 */
public class MainViewerController implements Initializable, ProgressUpdateCallback {

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

        SplitPaneDividerSlider slider = new SplitPaneDividerSlider(consoleSplit, 0, SplitPaneDividerSlider.Direction.DOWN);

        consoleButton.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) -> {
            slider.setAimContentVisible(t1);
        });
        slider.setAimContentVisible(false);
    }

    @FXML
    private void onLoadDirectory(ActionEvent event) {

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select numass storage root");
        chooser.setInitialDirectory(new File(".").getAbsoluteFile());

        final File rootDir = chooser.showDialog(((Node) event.getTarget()).getScene().getWindow());

        if (rootDir != null) {
            storagePathLabel.setText("Storage: " + rootDir.getAbsolutePath());
            setProgress(-1);
            setProgressText("Building numass storage tree...");
            new Thread(() -> {
                try {

                    NumassStorage root = NumassStorage.buildLocalNumassRoot(rootDir, true);
                    setRootStorage(root);

                } catch (StorageException ex) {
                    setProgress(0);
                    setProgressText("Failed to load local storage");
                    Logger.getLogger(MainViewerController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }, "loader thread").start();
        }

    }

    @Override
    public void setProgress(double progress) {
        Platform.runLater(() -> statusBar.setProgress(progress));
        //statusBar.setProgress(progress);
    }

    @Override
    public void setProgressText(String text) {
        Platform.runLater(() -> statusBar.setText(text));
        //statusBar.setText(text);
    }

    public void setRootStorage(NumassStorage root) {
        fillNumassStorageData(root);
        if (mspController != null) {
            mspController.fillMspData(root);
        } else {
            mspTab.getContent().setVisible(false);
        }

        pressuresTab.getContent().setVisible(false);
        temperaturesTab.getContent().setVisible(false);

    }

    private void fillNumassStorageData(NumassStorage rootStorage) {
        if (rootStorage != null) {
            setProgress(-1);
            setProgressText("Loading numass storage tree...");

            try {
                new NumassLoaderTreeBuilder(MainViewerController.this).fillTree(numassLoaderDataTree, rootStorage, (NumassData loader) -> {
                    NumassLoaderViewComponent component = NumassLoaderViewComponent.build(loader);
                    numassLoaderViewContainer.getChildren().clear();
                    numassLoaderViewContainer.getChildren().add(component);
                    AnchorPane.setTopAnchor(component, 0.0);
                    AnchorPane.setRightAnchor(component, 0.0);
                    AnchorPane.setLeftAnchor(component, 0.0);
                    AnchorPane.setBottomAnchor(component, 0.0);
                    numassLoaderViewContainer.requestLayout();
                });
                setProgress(0);
                setProgressText("Loaded");
            } catch (StorageException ex) {
                throw new RuntimeException(ex);
            }
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
        Platform.runLater(() -> storageText.requestFocus());

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(storageText.getText(), runText.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        if (result.isPresent()) {
            storagePathLabel.setText("Storage: remote/" + result.get().getValue());

            setProgress(-1);
            setProgressText("Building numass storage tree...");
            new Thread(() -> {
                try {
                    NumassStorage root = NumassStorage.buildRemoteNumassRoot(result.get().getKey() + "/data/" + result.get().getValue());
                    setRootStorage(root);
                } catch (StorageException ex) {
                    setProgress(0);
                    setProgressText("Failed to load remote storage");
                    Logger.getLogger(MainViewerController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }, "loader thread").start();

        }
    }
}
