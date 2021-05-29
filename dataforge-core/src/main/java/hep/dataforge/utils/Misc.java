/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.utils;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;

/**
 * @author Alexander Nozik
 */
public class Misc {
    public static final Charset UTF = Charset.forName("UTF-8");

    /**
     * A synchronized lru cache
     *
     * @param <K>
     * @param <V>
     * @param maxItems
     * @return
     */
    public static <K, V> Map<K, V> getLRUCache(int maxItems) {
        return Collections.synchronizedMap(new LinkedHashMap<K, V>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return super.size() > maxItems;
            }
        });
    }

    /**
     * Check if current thread is interrupted and throw exception if it is
     */
    public static void checkThread() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException();
        }
    }
}
