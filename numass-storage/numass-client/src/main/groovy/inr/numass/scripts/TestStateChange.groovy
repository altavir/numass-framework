/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package inr.numass.scripts

import hep.dataforge.io.MetaStreamReader
import hep.dataforge.io.MetaStreamWriter
import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.io.envelopes.EnvelopeBuilder
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.storage.commons.JSONMetaWriter
import hep.dataforge.storage.commons.StorageManager
import java.io.RandomAccessFile;
import java.nio.ByteBuffer
import inr.numass.client.NumassClient
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import hep.dataforge.meta.MetaBuilder



new StorageManager().startGlobal();

new NumassClient("127.0.0.1",8335).withCloseable{

    MetaStreamWriter parser = new JSONMetaWriter();

    Meta startRun = it.startRun("test")

    println parser.writeString(startRun);
    
    Meta set1 = it.setState("myState", 112);
    
    println parser.writeString(set1);
    
    Meta set2 = it.setState("otherState.property", ["a", "b", "c"])
    
    println it.getStates()
}
