package inr.numass.control;

import ch.qos.logback.classic.Level;
import hep.dataforge.context.Context;
import hep.dataforge.control.connections.DeviceConnection;
import hep.dataforge.control.connections.Roles;
import hep.dataforge.control.devices.Device;
import hep.dataforge.control.devices.DeviceFactory;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.meta.Meta;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Objects;

/**
 * Created by darksnake on 14-May-17.
 */
public abstract class NumassControlApplication<D extends Device> extends Application {
    private D device;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Locale.setDefault(Locale.US);// чтобы отделение десятичных знаков было точкой
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.INFO);

        DeviceViewConnection<D> controller = buildView();
        BorderPane pane = new BorderPane();
        pane.setCenter(controller.getFXNode());

        Scene scene = new Scene(pane);

        primaryStage.setScene(scene);
        primaryStage.show();

        setupDevice(controller);
    }

    /**
     * Build a view connection
     *
     * @return
     */
    protected abstract DeviceViewConnection<D> buildView();

    /**
     * Get a device factory for given device
     *
     * @return
     */
    protected abstract DeviceFactory<D> getDeviceFactory();

    protected abstract void setupStage(Stage stage);


    private void setupDevice(DeviceConnection<D> controller) {
        Meta config = NumassControlUtils.getConfig(this)
                .orElseGet(() -> NumassControlUtils.readResourceMeta("/config/msp-config.xml"));

        Context ctx = NumassControlUtils.setupContext(config);
        Meta mspConfig = NumassControlUtils.findDeviceMeta(config, it -> Objects.equals(it.getString("name"), "msp"))
                .orElseThrow(() -> new RuntimeException("Msp configuration not found"));


        Platform.runLater(() -> {
            try {
                device = getDeviceFactory().build(ctx, mspConfig);
                device.init();
                device.connect(controller, Roles.VIEW_ROLE, Roles.DEVICE_LISTENER_ROLE);
                NumassControlUtils.connectStorage(device, config);
            } catch (ControlException e) {
                throw new RuntimeException("Failed to build device", e);
            }
        });
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (device != null) {
            device.shutdown();
            device.getContext().close();
        }
    }


}
