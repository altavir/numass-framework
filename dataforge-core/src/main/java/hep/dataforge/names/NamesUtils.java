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
package hep.dataforge.names;

import hep.dataforge.exceptions.NameNotFoundException;
import hep.dataforge.values.Values;

import java.util.ArrayList;
import java.util.List;

import static java.lang.System.arraycopy;
import static java.util.Arrays.asList;

/**
 * <p>
 * NamesUtils class.</p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class NamesUtils {

    /**
     * проверка того, что два набора имен полностью совпадают с точностью до
     * порядка
     *
     * @param names1 an array of {@link java.lang.String} objects.
     * @param names2 an array of {@link java.lang.String} objects.
     * @return a boolean.
     */
    public static boolean areEqual(String[] names1, String[] names2) {
        if (names1.length != names2.length) {
            return false;
        }
        for (int i = 0; i < names2.length; i++) {
            if (!names1[i].equals(names2[i])) {
                return false;
            }

        }
        return true;
    }

    /**
     * Проверка того, что два Names содержат одинаковый набор имен, без учета
     * порядка.
     *
     * @param named1 a {@link hep.dataforge.names.NameList} object.
     * @param named2 a {@link hep.dataforge.names.NameList} object.
     * @return a boolean.
     */
    public static boolean areEqual(NameList named1, NameList named2) {
        return (named1.contains(named2)) && (named2.contains(named1));
    }

    /**
     * <p>
     * combineNames.</p>
     *
     * @param names1 an array of {@link java.lang.String} objects.
     * @param names2 a {@link java.lang.String} object.
     * @return an array of {@link java.lang.String} objects.
     */
    public static String[] combineNames(String[] names1, String... names2) {

        /*
         * Не обязательная дорогая проверка, чтобы исключить дублирование имен
         */
        if (!notIncludesEquals(names1, names2)) {
            throw new IllegalArgumentException("Names must be different.");
        }
        String[] res = new String[names1.length + names2.length];
        arraycopy(names1, 0, res, 0, names1.length);
        arraycopy(names2, 0, res, names1.length, names2.length);
        return res;
    }

    /**
     * Собирает из двух массивов имен один, при этом убирает дублирующиеся имена
     *
     * @param names1 an array of {@link java.lang.String} objects.
     * @param names2 a {@link java.lang.String} object.
     * @return an array of {@link java.lang.String} objects.
     */
    public static String[] combineNamesWithEquals(String[] names1, String... names2) {
        ArrayList<String> strings = new ArrayList<>();
        strings.addAll(asList(names1));
        for (String name : names2) {
            if (!strings.contains(name)) {
                strings.add(name);
            }
        }
        String[] res = new String[strings.size()];
        return strings.toArray(res);
    }

    /**
     * <p>
     * contains.</p>
     *
     * @param nameList an array of {@link java.lang.String} objects.
     * @param name     a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean contains(String[] nameList, String name) {
        for (String nameList1 : nameList) {
            if (nameList1.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * exclude.</p>
     *
     * @param named       a {@link hep.dataforge.names.NameList} object.
     * @param excludeName a {@link java.lang.String} object.
     * @return an array of {@link java.lang.String} objects.
     */
    public static String[] exclude(NameList named, String excludeName) {
        List<String> names = named.asList();
        names.remove(excludeName);
        return names.toArray(new String[names.size()]);
    }

    /**
     * <p>
     * exclude.</p>
     *
     * @param names       an array of {@link java.lang.String} objects.
     * @param excludeName a {@link java.lang.String} object.
     * @return an array of {@link java.lang.String} objects.
     */
    public static String[] exclude(String[] names, String excludeName) {
        ArrayList<String> list = new ArrayList<>();
        for (String name : names) {
            if (!name.equals(excludeName)) {
                list.add(name);
            }
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * TODO replace by List
     *
     * @param set
     * @return an array of {@link java.lang.Number} objects.
     * @throws hep.dataforge.exceptions.NameNotFoundException if any.
     */
    public static Number[] getAllNamedSetValues(Values set) throws NameNotFoundException {
        Number[] res = new Number[set.getNames().size()];
        List<String> names = set.getNames().asList();
        for (int i = 0; i < set.getNames().size(); i++) {
            res[i] = set.getValue(names.get(i)).getDouble();
        }
        return res;
    }

    /**
     * <p>
     * getNamedSubSetValues.</p>
     *
     * @param set
     * @param names a {@link java.lang.String} object.
     * @return an array of double.
     * @throws hep.dataforge.exceptions.NameNotFoundException if any.
     */
    public static double[] getNamedSubSetValues(Values set, String... names) throws NameNotFoundException {
        double[] res = new double[names.length];
        for (int i = 0; i < names.length; i++) {
            res[i] = set.getValue(names[i]).getDouble();

        }
        return res;
    }

    /**
     * Проверка того, что два набора имен не пересекаются
     *
     * @param names1 an array of {@link java.lang.String} objects.
     * @param names2 an array of {@link java.lang.String} objects.
     * @return a boolean.
     */
    public static boolean notIncludesEquals(String[] names1, String[] names2) {
        for (String names11 : names1) {
            for (String names21 : names2) {
                if (names11.equalsIgnoreCase(names21)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Generate a default axis name set for given number of dimensions
     *
     * @param dim
     * @return
     */
    public static NameList generateNames(int dim) {
        switch (dim) {
            case 0:
                return new NameList();
            case 1:
                return new NameList("x");
            case 2:
                return new NameList("x", "y");
            case 3:
                return new NameList("x", "y", "z");
            default:
                List<String> names = new ArrayList<>();
                for (int i = 0; i < dim; i++) {
                    names.add("axis_" + (i + 1));
                }
                return new NameList(names);
        }
    }

    /**
     * Check if given string is a valid size=1 name
     * @param token
     * @return
     */
    public static boolean isValidToken(String token){
        try {
            return Name.Companion.of(token).getLength() == 1;
        }catch (Exception ex){
            return false;
        }
    }

}
