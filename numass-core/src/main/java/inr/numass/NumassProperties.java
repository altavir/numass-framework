/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass;

import hep.dataforge.context.Global;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author Alexander Nozik
 */
public class NumassProperties {

    private static File getNumassPropertiesFile() throws IOException {
        File file = new File(Global.INSTANCE.getUserDirectory(), "numass");
        if (!file.exists()) {
            file.mkdirs();
        }
        file = new File(file, "numass.cfg");
        if(!file.exists()){
            file.createNewFile();
        }
        return file;
    }

    public static String getNumassProperty(String key) {
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(getNumassPropertiesFile()));
            return props.getProperty(key);
        } catch (IOException ex) {
            return null;
        }
    }

    public synchronized static void setNumassProperty(String key, @Nullable String value) {
        try {
            Properties props = new Properties();
            File store = getNumassPropertiesFile();
            props.load(new FileInputStream(store));
            if(value == null){
                props.remove(key);
            } else {
                props.setProperty(key, value);
            }
            props.store(new FileOutputStream(store), "");
        } catch (IOException ex) {
            Global.INSTANCE.getLogger().error("Failed to save numass properties", ex);
        }
    }
}
