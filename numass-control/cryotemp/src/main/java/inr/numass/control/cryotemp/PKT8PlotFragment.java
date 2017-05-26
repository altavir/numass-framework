package inr.numass.control.cryotemp;

import hep.dataforge.control.connections.Roles;
import hep.dataforge.fx.fragments.FXFragment;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

/**
 * Created by darksnake on 07-Oct-16.
 */
public class PKT8PlotFragment extends FXFragment {
    private PKT8PlotView plotController;

    public PKT8PlotFragment(PKT8Device device) {
        super("PKT8 cryogenic temperature viewer", 600, 400);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PKT8Plot.fxml"));
            loader.load();
            plotController = loader.getController();
            device.connect(plotController, Roles.VIEW_ROLE);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//        showingProperty().addListener((observable, oldValue, newValue) -> {
//            if (device.isMeasuring()) {
//                if (newValue) {
//                    device.getMeasurement().addListener(plotController);
//                } else {
//                    device.getMeasurement().removeListener(plotController);
//                }
//            }
//        });
    }

    @Override
    protected Parent buildRoot() {
        return plotController.getPane();
    }
}
