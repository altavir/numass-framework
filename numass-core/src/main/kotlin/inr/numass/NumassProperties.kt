/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass

import hep.dataforge.context.Global
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

/**
 *
 * @author Alexander Nozik
 */
object NumassProperties {

    private val numassPropertiesFile: File
        @Throws(IOException::class)
        get() {
            var file = File(Global.userDirectory, "numass")
            if (!file.exists()) {
                file.mkdirs()
            }
            file = File(file, "numass.cfg")
            if (!file.exists()) {
                file.createNewFile()
            }
            return file
        }

    fun getNumassProperty(key: String): String? {
        try {
            val props = Properties()
            props.load(FileInputStream(numassPropertiesFile))
            return props.getProperty(key)
        } catch (ex: IOException) {
            return null
        }

    }

    @Synchronized
    fun setNumassProperty(key: String, value: String?) {
        try {
            val props = Properties()
            val store = numassPropertiesFile
            props.load(FileInputStream(store))
            if (value == null) {
                props.remove(key)
            } else {
                props.setProperty(key, value)
            }
            props.store(FileOutputStream(store), "")
        } catch (ex: IOException) {
            Global.logger.error("Failed to save numass properties", ex)
        }

    }
}
