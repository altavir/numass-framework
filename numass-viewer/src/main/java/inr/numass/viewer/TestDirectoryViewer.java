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

import hep.dataforge.storage.commons.StorageManager;
import inr.numass.data.storage.NumassDataLoader;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author darksnake
 */
public class TestDirectoryViewer extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        new StorageManager().startGlobal();

        NumassDataLoader reader = NumassDataLoader.fromDir(null, Paths.get("C:\\Users\\darksnake\\Dropbox\\PlayGround\\data-test\\20150703143643_1\\"), null);
//        NumassLoader reader = NumassLoader.fromZip(null, new File("C:\\Users\\darksnake\\Dropbox\\PlayGround\\data-test\\20150703143643_1.zip"));

        NumassLoaderView comp = new NumassLoaderView();
        comp.loadData(reader);
//        FXMLLoader fxml = new FXMLLoader(getClass().getResource("/fxml/DirectoryViewer.fxml"));
//
//        Parent parent = fxml.load();
//
//        NumassLoaderViewController controller = fxml.getController();
//
//        controller.setModel(reader);

        Scene scene = new Scene(comp.getRoot(), 800, 600);

        stage.setTitle("Detector Visualisation test");
        stage.setScene(scene);
        stage.setMinHeight(600);
        stage.setMinWidth(800);
//        primaryStage.setResizable(false);

        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
