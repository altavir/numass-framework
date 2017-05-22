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
package inr.numass.cryotemp;

import hep.dataforge.control.devices.DeviceFactory;
import hep.dataforge.meta.Meta;
import inr.numass.control.DeviceViewConnection;
import inr.numass.control.NumassControlApplication;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * @author darksnake
 */
public class PKT8App extends NumassControlApplication<PKT8Device> {
    @Override
    protected DeviceViewConnection<PKT8Device> buildView() {
        return PKT8View.build();
    }

    @Override
    protected DeviceFactory<PKT8Device> getDeviceFactory() {
        return new PKT8DeviceFactory();
    }

    @Override
    protected void setupStage(Stage stage, PKT8Device device) {
        stage.setTitle("Numass temperature view " + device.getName());
        stage.setMinHeight(400);
        stage.setMinWidth(400);
    }

    @Override
    protected boolean acceptDevice(Meta meta) {
        return Objects.equals(meta.getString("type"), "PKT8");
    }


}
