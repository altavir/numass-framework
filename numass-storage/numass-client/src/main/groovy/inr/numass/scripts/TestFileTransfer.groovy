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
import hep.dataforge.storage.commons.JSONMetaWriter
import hep.dataforge.storage.commons.StorageManager
import java.io.RandomAccessFile;
import java.nio.ByteBuffer
import inr.numass.client.NumassClient
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel


new StorageManager().startGlobal();

new NumassClient("127.0.0.1",8335).withCloseable{

    MetaStreamWriter parser = new JSONMetaWriter();

    Meta startRun = it.startRun("test")

    println parser.writeString(startRun);

    Meta run = it.getCurrentRun();
    println parser.writeString(run);
    
    Meta response = it.sendNumassData("C:\\Users\\darksnake\\Dropbox\\PlayGround\\data-test\\zip\\20150703143643_1.nm.zip");
    
    println parser.writeString(response);

    response = it.sendNumassData("C:\\Users\\darksnake\\Dropbox\\PlayGround\\data-test\\20150703144707_2");
    
    println parser.writeString(response);
}
 