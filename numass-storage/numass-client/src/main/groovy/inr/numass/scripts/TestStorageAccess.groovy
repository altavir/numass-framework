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
import hep.dataforge.storage.commons.StoragePlugin
import java.io.RandomAccessFile;
import java.nio.ByteBuffer
import inr.numass.client.NumassClient
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import hep.dataforge.data.MapDataPoint
import hep.dataforge.storage.commons.LoaderFactory
import hep.dataforge.data.DataFormat
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.data.DataPoint


new StoragePlugin().startGlobal();

new NumassClient("127.0.0.1",8335).withCloseable{

    MetaStreamWriter parser = new JSONMetaWriter();

    Meta startRun = it.startRun("test")

    println parser.writeString(startRun);
    
    
    MetaBuilder target = new MetaBuilder("target")
    .setValue("type","loader")
    .setValue("name", "testPointLoader")
    .putNode(LoaderFactory.buildDataPointLoaderMeta("testPointLoader","a", DataFormat.forNames("a", "b", "c")).rename("meta"))
    
    MetaBuilder data = new MetaBuilder("data");
    
    String[] names = ["a","b","c"]
    
    for(int i = 0; i<5; i++){
        data.putNode(DataPoint.toMeta(new MapDataPoint(names,i, 2*i,3*i)));
    }
    
   
    Envelope bin = it.requestBase("numass.storage")
    .putMetaValue("action","push")
    .putMetaNode(target)
    .putMetaNode(data)
    .build();
        
    
    def response = it.sendAndRecieve(bin);
    
    println parser.writeString(response.meta());

}