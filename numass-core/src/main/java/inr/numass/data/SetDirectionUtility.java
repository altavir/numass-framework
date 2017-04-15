/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.data;

import hep.dataforge.context.Context;
import hep.dataforge.context.Global;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A temporary utility to store set directions to avoid multiple file reading
 *
 * @author Alexander Nozik
 */
public class SetDirectionUtility {

    private static final String FILE_NAME = "numass_set_direction.map";

    private static final Map<String, Boolean> directionMap = new HashMap<>();

    private static boolean isLoaded = false;

    static synchronized boolean isReversed(String setName, Function<String, Boolean> provider) {
        if (!isLoaded) {
            load(Global.instance());
        }
        return directionMap.computeIfAbsent(setName, provider);
    }

    public static File cacheFile(Context context) {
        return new File(context.io().getTmpDirectory(), FILE_NAME);
    }

    @SuppressWarnings("unchecked")
    public static synchronized void load(Context context) {
        context.getLogger().info("Loading set direction utility");
        File file = cacheFile(context);
        if (file.exists()) {
            directionMap.clear();
            try (FileInputStream fst = new FileInputStream(file)) {
                try (ObjectInputStream st = new ObjectInputStream(fst)) {
                    directionMap.putAll((Map<String, Boolean>) st.readObject());
                    context.getLogger().info("Set directions successfully loaded from file");
                } catch (ClassNotFoundException | IOException ex) {
                    context.getLogger().error("Failed to load numass direction mapping", ex);
                }
            } catch (IOException ex) {
                 context.getLogger().error("Failed to load numass direction mapping", ex);
            }
        }

        isLoaded = true;
    }

    public static synchronized void save(Context context) {
        try {
            File file = cacheFile(context);
            if (!file.exists()) {
                file.createNewFile();
            }
            try (ObjectOutputStream st = new ObjectOutputStream(new FileOutputStream(file))) {
                st.writeObject(directionMap);
                context.getLogger().info("Set directions successfully saved to file");
            }
        } catch (IOException ex) {
            context.getLogger().error("Failed to save numass direction mapping", ex);
        }
    }
}
