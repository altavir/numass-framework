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
import javafx.stage.Stage;
import org.slf4j.LoggerFactory;

import java.util.Locale;

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

        Scene scene = new Scene(controller.getPane());

        primaryStage.setScene(scene);
        primaryStage.show();

        device = setupDevice(controller);
        setupStage(primaryStage, device);
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

    protected abstract void setupStage(Stage stage, D device);

    protected abstract boolean acceptDevice(Meta meta);

    private D setupDevice(DeviceConnection<D> controller) {
        Meta config = NumassControlUtils.getConfig(this)
                .orElseGet(() -> NumassControlUtils.readResourceMeta("/config/devices.xml"));

        Context ctx = NumassControlUtils.setupContext(config);
        Meta deviceConfig = NumassControlUtils.findDeviceMeta(config, this::acceptDevice)
                .orElseThrow(() -> new RuntimeException("Device configuration not found"));


        try {
            D d = getDeviceFactory().build(ctx, deviceConfig);
            d.init();
            NumassControlUtils.connectStorage(d, config);
            Platform.runLater(() -> {
                d.connect(controller, Roles.VIEW_ROLE, Roles.DEVICE_LISTENER_ROLE);
            });
            return d;
        } catch (ControlException e) {
            throw new RuntimeException("Failed to build device", e);
        }
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
