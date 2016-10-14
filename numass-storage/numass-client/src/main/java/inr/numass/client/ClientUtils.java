package inr.numass.client;

import hep.dataforge.meta.Meta;

import java.io.IOException;

/**
 * Created by darksnake on 09-Oct-16.
 */
public class ClientUtils {
    public static String getRunName(Meta config) {
        if (config.hasValue("numass.run")) {
            return config.getString("numass.run");
        } else if (config.hasMeta("numass.server")) {
            try {
                return new NumassClient(config.getMeta("numass.server")).getCurrentRun().getString("path");
            } catch (IOException e) {
                return "";
            }
        } else {
            return "";
        }
    }
}
