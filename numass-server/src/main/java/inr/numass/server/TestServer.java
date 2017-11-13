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
import hep.dataforge.storage.filestorage.FileStorageFactory;
import inr.numass.data.storage.NumassStorage;

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
        Context context = Global.getContext("NUMASS-SERVER");

        StorageManager storageManager = context.getPluginManager().load(StorageManager.class);

        ServerManager serverManager = context.getPluginManager().load(ServerManager.class);

        File path = new File("/D:/temp/test");
        context.getLogger().info("Starting test numass storage servlet in '{}'", path);

        NumassStorage storage = new NumassStorage(context, FileStorageFactory.buildStorageMeta(path, true, true));
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
