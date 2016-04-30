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
package inr.numass.readvac;

import hep.dataforge.exceptions.StorageException;
import hep.dataforge.meta.Meta;
import hep.dataforge.storage.api.PointLoader;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.commons.LoaderFactory;
import hep.dataforge.tables.DataPoint;
import hep.dataforge.tables.TableFormatBuilder;
import hep.dataforge.values.ValueType;
import java.time.Instant;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.SwingUtilities;
import jssc.SerialPortException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Darksnake
 */
public class VACManager implements AutoCloseable {

    public static String[] names = {"timestamp", "P1", "P2", "P3", "Px"};

    private VACFrame frame;

    private Timer timer;

    private int timerInterval = 5000;

//    private boolean isValuesShowing;
//    private boolean isLogShowing;
    private final PointLoader loader;
    private VACDeviceReader device;
    private final Iterator<DataPoint> reader; // Чтение из файла или непосредтсвенно с прибора

    private Instant lastUpdate;

//    public static VACManager fromDirectory(Storage server, String sourceDir, String runPrefix) throws StorageException, FileNotFoundException {
//        return new VACManager(setupLoader(server, runPrefix), VACFileReader.fromDirectory(sourceDir));
//    }
    public static VACManager fromSerial(Storage server, String run, Meta serialConfig) throws StorageException, SerialPortException {
        return new VACManager(setupLoader(server, run), new VACDeviceReader(serialConfig));
    }

    public VACManager(PointLoader loader, Iterator<DataPoint> reader) {
        this.loader = loader;
        this.reader = reader;
        showPlot();
    }

    public VACManager(PointLoader loader, VACDeviceReader reader) {
        this.loader = loader;
        this.reader = reader;
        this.device = reader;
        showPlot();
    }

    private static PointLoader setupLoader(Storage storage, String run) throws StorageException {
        return LoaderFactory.buildPointLoder(storage, "vactms", run, "timestamp",
                new TableFormatBuilder(names)
                .setType("timestamp", ValueType.TIME)
                .build());
    }

    /**
     * @return the timerInterval
     */
    public int getTimerInterval() {
        return timerInterval;
    }

    /**
     * Интервал в милисекундах
     *
     * @param millis
     */
    public void setTimerInterval(int millis) {
        this.timerInterval = millis;
        //Перезапускаем таймер, чтобы обновились интервалы
        stop();
        start();
//        setAutoRange(millis*500);
    }

    public void start() {
        timer = new Timer("UpdateTimer");
        timer.scheduleAtFixedRate(getTimerTask(), 0, getTimerInterval());
    }

    public void stop() {
        timer.cancel();
    }

    @Override
    public void close() throws Exception {
        stop();
        if (device != null) {
            device.close();
        }
        loader.close();
    }

    public final void showPlot() {
        if (frame != null) {
            if (frame.isDisplayable()) {
                frame.setVisible(true);
            } else {
                //Подчищаем неправильно закрытые окна
                frame.dispose();
                frame = null;
            }
        }
        if (frame == null) {
            SwingUtilities.invokeLater(() -> {
                frame = VACFrame.display(VACManager.this);
            });

        }
    }

    public DataPoint readPoint() {
        return reader.next();
    }

    public boolean hasNextPoint() {
        return reader.hasNext();
    }

    /**
     * Пропускаем все точки, до определенного момента. Возвращаем последнюю
     * точку в списке, или первую, время которой больше, чем у заданной
     *
     * @param time
     * @return
     */
    public DataPoint skipUntil(Instant time) {
        if (reader.hasNext()) {
            DataPoint point = reader.next();
            while (point.getValue("timestamp").timeValue().isBefore(time) && reader.hasNext()) {
                point = reader.next();
            }
            return point;
        } else {
            return null;
        }
    }

    public boolean p1Available() {
        return (device != null) && (device.isP1Available());
    }

    public boolean getP1PowerState() throws P1ControlException {
        try {
            if (device == null) {
                throw new P1ControlException("P1 control is not initialized");
            }
            return device.getP1PowerState();
        } catch (SerialPortException | ResponseParseException ex) {
            throw new P1ControlException("Can't read P1 answer");
        }
    }

    public boolean setP1PowerStateOn(boolean state) throws P1ControlException {
        try {
            if (device == null) {
                throw new P1ControlException("P1 control is not initialized");
            }
            return device.setP1PowerStateOn(state);
        } catch (SerialPortException | ResponseParseException ex) {
            throw new P1ControlException("Can't read P1 answer");
        }
    }

    private TimerTask getTimerTask() {
        return new TimerTask() {

            @Override
            public void run() {
                try {
//                    while (hasNextPoint()) {
                    if (hasNextPoint()) {
                        DataPoint point = readPoint();
                        // На всякий случай берем время на секунду назад, чтобы не было накладок с пропусками
//                        DataPoint point = skipUntil(Instant.now().minusSeconds(1));
                        //Проверяем, что точка получена
                        if (point != null) {
                            //Если точка старая, то не обновляем ничего
                            Instant pointTime = point.getValue("timestamp").timeValue();
                            if (lastUpdate != null && pointTime.isAfter(lastUpdate)) {
                                loader.push(point);
                                if (frame != null) {
                                    frame.displayPoint(point);
                                }
                            }
                            lastUpdate = pointTime;
                        }
                    }
                } catch (Exception ex) {
                    LoggerFactory.getLogger(VACManager.class).error("Unexpected error during point aquisition", ex);
                }
            }
        };
    }

}
