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
import hep.dataforge.context.GlobalContext;
import hep.dataforge.context.ProcessManager;
import hep.dataforge.exceptions.StorageException;
import hep.dataforge.fx.ConsoleFragment;
import hep.dataforge.fx.process.ProcessManagerFragment;
import inr.numass.NumassProperties;
import inr.numass.storage.NumassData;
import inr.numass.storage.NumassStorage;
import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
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
public class MainViewerController implements Initializable {

    public static MainViewerController build(NumassStorage root) {
        MainViewerController res = new MainViewerController();
        res.setRootStorage(root);
        return res;
    }

//    private ConsoleFragment consoleFragment;
//    private ProcessManagerFragment processFragment = ProcessManagerFragment.attachToContext(GlobalContext.instance());
    @FXML
    private ToggleButton consoleButton;
    @FXML
    private Button loadDirectoryButton;

    private MspViewController mspController;

    @FXML
    private AnchorPane mspPlotPane;

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
    private ToggleButton processManagerButton;

//    private Popup progressPopup;
    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ConsoleFragment consoleFragment = new ConsoleFragment();
        consoleFragment.hookStd();
        consoleFragment.bindTo(consoleButton);
        ProcessManagerFragment.attachToContext(GlobalContext.instance()).bindTo(processManagerButton);
    }

    @FXML
    private void onLoadDirectory(ActionEvent event) {

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select numass storage root");
        String storageRoot = NumassProperties.getNumassProperty("numass.storage.root");
        if (storageRoot == null) {
            chooser.setInitialDirectory(new File(".").getAbsoluteFile());
        } else {
            chooser.setInitialDirectory(new File(storageRoot));
        }

        final File rootDir = chooser.showDialog(((Node) event.getTarget()).getScene().getWindow());

        if (rootDir != null) {
            NumassProperties.setNumassProperty("numass.storage.root", rootDir.getAbsolutePath());
            loadDirectory(rootDir.toURI().toString());
        }
    }

    private void loadDirectory(String path) {
        getContext().processManager().post("viewer.loadDirectory", (ProcessManager.Callback callback) -> {
            callback.updateTitle("Load storage (" + path + ")");
            callback.setProgress(-1);
            callback.updateMessage("Building numass storage tree...");
            try {
                NumassStorage root = NumassStorage.buildNumassRoot(path, true, false);
                setRootStorage(root);
                Platform.runLater(() -> storagePathLabel.setText("Storage: " + path));
            } catch (StorageException ex) {
                callback.setProgress(0);
                callback.updateMessage("Failed to load storage " + path);
                Logger.getLogger(MainViewerController.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }

    private Context getContext() {
        return GlobalContext.instance();
    }

    public void setRootStorage(NumassStorage root) {

        getContext().processManager().cleanup();
        getContext().processManager().post("viewer.storage.load", (ProcessManager.Callback callback) -> {
            callback.updateTitle("Fill data to UI (" + root.getName() + ")");
            callback.setProgress(-1);
            Platform.runLater(() -> statusBar.setProgress(-1));

            callback.updateMessage("Loading numass storage tree...");

            try {
                new NumassLoaderTreeBuilder().build(callback, numassLoaderDataTree, root, (NumassData loader) -> {
                    NumassLoaderViewComponent component = new NumassLoaderViewComponent(getContext());
                    component.loadData(loader);
                    numassLoaderViewContainer.getChildren().clear();
                    numassLoaderViewContainer.getChildren().add(component);
                    AnchorPane.setTopAnchor(component, 0.0);
                    AnchorPane.setRightAnchor(component, 0.0);
                    AnchorPane.setLeftAnchor(component, 0.0);
                    AnchorPane.setBottomAnchor(component, 0.0);
                    numassLoaderViewContainer.requestLayout();
                });
            } catch (StorageException ex) {
                Logger.getLogger(MainViewerController.class.getName()).log(Level.SEVERE, null, ex);
            }

//            callback.setProgress(1, 1);
            Platform.runLater(() -> statusBar.setProgress(0));
            callback.updateMessage("Numass storage tree loaded.");
            callback.setProgressToMax();
        });

        mspController = new MspViewController(getContext(), mspPlotPane);
        mspController.fillMspData(root);

        pressuresTab.getContent().setVisible(false);
        temperaturesTab.getContent().setVisible(false);

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
            String path = result.get().getKey() + "/data/" + result.get().getValue();
            loadDirectory(path);
        }
    }
}
