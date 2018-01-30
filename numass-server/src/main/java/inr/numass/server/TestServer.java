/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and read the template in the editor.
 */
package inr.numass.server;

import hep.dataforge.context.Context;
import hep.dataforge.context.Global;
import hep.dataforge.server.ServerManager;
import hep.dataforge.server.storage.StorageServerUtils;
import hep.dataforge.storage.commons.StorageManager;
import inr.numass.data.storage.NumassStorage;
import inr.numass.data.storage.NumassStorageFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * @author Alexander Nozik
 */
public class TestServer {

    /**
     * @param args the command line arguments
     * @throws hep.dataforge.exceptions.StorageException
     */
    public static void main(String[] args) throws Exception {
        Context context = Global.Companion.getContext("NUMASS-SERVER");

        StorageManager storageManager = context.getPluginManager().load(StorageManager.class);

        ServerManager serverManager = context.getPluginManager().load(ServerManager.class);

        File path = new File("/D:/temp/test");
        context.getLogger().info("Starting test numass storage servlet in '{}'", path);

        NumassStorage storage = (NumassStorage) storageManager.buildStorage(NumassStorageFactory.buildStorageMeta(path.toURI(), true, true));
        StorageServerUtils.addStorage(serverManager,storage,"numass-storage");

        serverManager.startServer();

        String stopLine = "";

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (stopLine == null || !stopLine.startsWith("exit")) {
            //    print ">"
            stopLine = br.readLine();
        }

        System.out.println("Stopping ratpack");
        serverManager.stopServer();
    }

}
