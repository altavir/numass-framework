/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package inr.numass.scripts

import hep.dataforge.io.JSONMetaWriter
import hep.dataforge.io.MetaStreamWriter
import hep.dataforge.meta.Meta
import hep.dataforge.storage.commons.StorageManager
import inr.numass.client.NumassClient

new StorageManager().startGlobal();

new NumassClient("127.0.0.1",8335).withCloseable{

    MetaStreamWriter writer = JSONMetaWriter.INSTANCE

    Meta startRun = it.startRun("test")

    println writer.writeString(startRun);

    Meta run = it.getCurrentRun();
    println writer.writeString(run);
    
    Meta response = it.sendNumassData("C:\\Users\\darksnake\\Dropbox\\PlayGround\\data-test\\zip\\20150703143643_1.nm.zip");
    
    println writer.writeString(response);

    response = it.sendNumassData("C:\\Users\\darksnake\\Dropbox\\PlayGround\\data-test\\20150703144707_2");
    
    println writer.writeString(response);
}
 