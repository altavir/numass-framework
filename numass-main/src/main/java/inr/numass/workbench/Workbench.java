/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.workbench;

import inr.numass.NumassContext;
import java.io.IOException;
import java.text.ParseException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


/**
 *
 * @author Alexander Nozik
 */
public class Workbench extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException, ParseException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NumassWorkbench.fxml"));
        Parent parent = loader.load();

        Scene scene = new Scene(parent, 800, 600);

        NumassWorkbenchController controller = loader.getController();
        controller.setContextFactory(NumassContext::new);
        
        primaryStage.setTitle("Numass workbench");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
