/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass;

import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.storage.commons.StorageDataFactory;
import hep.dataforge.workspace.BasicWorkspace;
import hep.dataforge.workspace.Workspace;

/**
 *
 * @author Alexander Nozik
 */
public class WorkspaceTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        String storagepath = "D:\\Work\\Numass\\data\\";
        Workspace workspace = BasicWorkspace.builder()
                .setContext(Numass.buildContext())
                .loadData("", new StorageDataFactory(), new MetaBuilder("storage").putValue("path", storagepath))
                
                .build();
    }
    
}
