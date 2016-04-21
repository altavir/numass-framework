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

import hep.dataforge.exceptions.StorageException;
import hep.dataforge.storage.commons.StorageManager;
import java.io.IOException;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author Alexander Nozik
 */
public class Viewer extends Application {

    @Override
    public void start(Stage primaryStage) throws StorageException, IOException {
        new StorageManager().startGlobal();

        FXMLLoader fxml = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));

        Parent parent = fxml.load();

        Scene scene = new Scene(parent, 1024, 768);

        primaryStage.setTitle("Numass repository viewer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
//        System.exit(0);
    }

//    public static void runTask(Task task) {
//        Thread th = new Thread(task);
//        th.setDaemon(true);
//        th.start();
//    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
