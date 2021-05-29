/* 
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hep.dataforge.utils;

import hep.dataforge.exceptions.NamingException;
import java.util.function.Predicate;

/**
 * TODO сменить название
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class NamingUtils {

    /**
     * <p>
     * getMainName.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getMainName(String name) {
        String[] parse = parseName(name);
        return parse[0];
    }

    /**
     * <p>
     * getSubName.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getSubName(String name) {
        int index = name.indexOf(".");
        if (index < 0) {
            return null;
        } else {
            return name.substring(index + 1);
        }
    }

    /**
     * <p>
     * parseArray.</p>
     *
     * @param array a {@link java.lang.String} object.
     * @return an array of {@link java.lang.String} objects.
     */
    public static String[] parseArray(String array) {
        String str = array.trim();
        String[] res;
        if (str.startsWith("[")) {
            if (str.endsWith("]")) {
                str = str.substring(1, str.length() - 1);
            } else {
                throw new NamingException("Wrong syntax in array of names");
            }
        }
        res = str.split(",");
        for (int i = 0; i < res.length; i++) {
            res[i] = res[i].trim();
        }

        return res;
    }

    /**
     * <p>
     * buildArray.</p>
     *
     * @param array an array of {@link java.lang.String} objects.
     * @return a {@link java.lang.String} object.
     */
    public static String buildArray(String[] array) {
        StringBuilder res = new StringBuilder();
        res.append("[");
        for (int i = 0; i < array.length - 1; i++) {
            res.append(array[i]);
            res.append(",");
        }
        res.append(array[array.length - 1]);
        res.append("]");
        return res.toString();
    }

    /**
     * <p>
     * parseName.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return an array of {@link java.lang.String} objects.
     */
    public static String[] parseName(String name) {
        return name.trim().split(".");
    }

    public static boolean wildcardMatch(String mask, String str) {
        return str.matches(mask.replace("?", ".?").replace("*", ".*?"));
    }

    public static Predicate<String> wildcardMatchCondition(String mask) {
        String pattern = mask.replace("?", ".?").replace("*", ".*?");
        return str -> str.matches(pattern);
    }
}
