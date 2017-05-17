/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.fx;

import hep.dataforge.control.devices.DeviceFactory;
import hep.dataforge.meta.Meta;
import inr.numass.control.DeviceViewConnection;
import inr.numass.control.NumassControlApplication;
import inr.numass.readvac.VacCollectorDevice;
import inr.numass.readvac.VacDeviceFactory;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * @author Alexander Nozik
 */
public class ReadVac extends NumassControlApplication<VacCollectorDevice> {
    @Override
    protected DeviceViewConnection<VacCollectorDevice> buildView() {
        return VacCollectorView.build();
    }

    @Override
    protected DeviceFactory<VacCollectorDevice> getDeviceFactory() {
        return new VacDeviceFactory();
    }

    @Override
    protected void setupStage(Stage stage, VacCollectorDevice device) {
        stage.setTitle("Numass vacuum measurements");
    }

    @Override
    protected boolean acceptDevice(Meta meta) {
        return Objects.equals(meta.getString("type", ""), "numass:vac");
    }


}
