package inr.numass.cryotemp;

import hep.dataforge.fx.FXFragment;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;

/**
 * Created by darksnake on 07-Oct-16.
 */
public class PKT8PlotFragment extends FXFragment {
    private final PKT8Device device;
    private PKT8PlotController plotController;

    public PKT8PlotFragment(PKT8Device device) {
        this.device = device;
    }

    public PKT8PlotFragment(Window window, PKT8Device device) {
        super(window);
        this.device = device;
    }

    @Override
    protected Stage buildStage(Parent root) {
        Stage stage = new Stage();
        Scene scene = new Scene(root, 600, 400);


        stage.setTitle("PKT8 cryogenic temperature viewer");
        stage.setScene(scene);
        stage.setMinHeight(400);
        stage.setMinWidth(600);
        stage.sizeToScene();

        return stage;
    }

    @Override
    protected Parent getRoot() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PKT8Plot.fxml"));
        plotController = new PKT8PlotController(device);
        loader.setController(plotController);
        try {
            return loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onShow() {
        super.onShow();
        if (device.isMeasuring()) {
            device.getMeasurement().addListener(plotController);
        }
    }

    @Override
    protected void onHide() {
        super.onHide();
        if (device.isMeasuring()) {
            device.getMeasurement().removeListener(plotController);
        }
    }
}
