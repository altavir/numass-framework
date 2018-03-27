/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass

import hep.dataforge.meta.MetaBuilder
import hep.dataforge.storage.commons.StorageDataFactory
import hep.dataforge.workspace.BasicWorkspace

/**
 *
 * @author Alexander Nozik
 */
object WorkspaceTest {

    /**
     * @param args the command line arguments
     */
    @JvmStatic
    fun main(args: Array<String>) {

        val storagepath = "D:\\Work\\Numass\\data\\"

        val workspace = BasicWorkspace.builder().apply {
            this.context = Numass.buildContext()
            data("", StorageDataFactory(), MetaBuilder("storage").putValue("path", storagepath))
        }.build()
    }

}
