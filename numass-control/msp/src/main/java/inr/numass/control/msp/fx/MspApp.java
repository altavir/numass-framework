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
package inr.numass.control.msp.fx;

import hep.dataforge.control.devices.DeviceFactory;
import hep.dataforge.meta.Meta;
import inr.numass.control.DeviceViewConnection;
import inr.numass.control.NumassControlApplication;
import inr.numass.control.msp.MspDevice;
import inr.numass.control.msp.MspDeviceFactory;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * @author darksnake
 */
public class MspApp extends NumassControlApplication<MspDevice> {

    @Override
    protected DeviceViewConnection<MspDevice> buildView() {
        return MspViewController.build();
    }

    @Override
    protected DeviceFactory<MspDevice> getDeviceFactory() {
        return new MspDeviceFactory();
    }

    @Override
    protected void setupStage(Stage stage, MspDevice device) {
        stage.setTitle("Numass mass-spectrometer view");
        stage.setMinHeight(400);
        stage.setMinWidth(600);
    }

    @Override
    protected boolean acceptDevice(Meta meta) {
        return Objects.equals(meta.getString("name"), "msp");
    }


}
