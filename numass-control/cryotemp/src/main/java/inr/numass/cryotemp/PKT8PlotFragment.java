package inr.numass.cryotemp;

import hep.dataforge.fx.fragments.Fragment;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

/**
 * Created by darksnake on 07-Oct-16.
 */
public class PKT8PlotFragment extends Fragment {
    private final PKT8Device device;
    private PKT8PlotController plotController;

    public PKT8PlotFragment(PKT8Device device) {
        super("PKT8 cryogenic temperature viewer", 600, 400);
        this.device = device;
        showingProperty().addListener((observable, oldValue, newValue) -> {
            if (device.isMeasuring()) {
                if (newValue) {
                    device.getMeasurement().addListener(plotController);
                } else {
                    device.getMeasurement().removeListener(plotController);
                }
            }
        });
    }

    @Override
    protected Parent buildRoot() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PKT8Plot.fxml"));
        plotController = new PKT8PlotController(device);
        loader.setController(plotController);
        try {
            return loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
