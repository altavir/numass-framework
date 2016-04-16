/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package inr.numass.scripts

import hep.dataforge.io.MetaStreamReader
import hep.dataforge.io.MetaStreamWriter
import hep.dataforge.meta.Meta
import hep.dataforge.storage.commons.JSONMetaWriter
import hep.dataforge.storage.commons.StorageManager
import inr.numass.client.NumassClient

new StorageManager().startGlobal();

MetaStreamWriter parser = new JSONMetaWriter();

NumassClient.runComand("127.0.0.1", 8336, "addNote", "This is my note with <strong>html</strong>");
