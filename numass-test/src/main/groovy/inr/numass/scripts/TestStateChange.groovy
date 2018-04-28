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
    
    Meta set1 = it.setState("myState", 112);
    
    println writer.writeString(set1);
    
    Meta set2 = it.setState("otherState.property", ["a", "b", "c"])
    
    println it.getStates()
}
