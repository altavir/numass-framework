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
            FXMLLoader loader = new FXMLLoader(device.getContext().getClassLoader().getResource("fxml/PKT8Plot.fxml"));
            loader.setClassLoader(device.getContext().getClassLoader());
            loader.load();
            plotController = loader.getController();
            device.connect(plotController, Roles.VIEW_ROLE);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Parent buildRoot() {
        return plotController.getPane();
    }
}
