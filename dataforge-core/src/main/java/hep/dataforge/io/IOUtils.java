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
package hep.dataforge.io;

import hep.dataforge.tables.ListTable;
import hep.dataforge.tables.NavigableValuesSource;
import hep.dataforge.tables.TableFormat;
import hep.dataforge.values.Value;
import hep.dataforge.values.ValueType;
import hep.dataforge.values.Values;

import java.io.*;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static hep.dataforge.values.Value.NULL_STRING;
import static java.util.regex.Pattern.compile;

/**
 * <p>
 * IOUtils class.</p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class IOUtils {
    public static final Charset ASCII_CHARSET = Charset.forName("US-ASCII");
    public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");


    /**
     * Constant <code>ANSI_RESET="\u001B[0m"</code>
     */
    public static final String ANSI_RESET = "\u001B[0m";
    /**
     * Constant <code>ANSI_BLACK="\u001B[30m"</code>
     */
    public static final String ANSI_BLACK = "\u001B[30m";
    /**
     * Constant <code>ANSI_RED="\u001B[31m"</code>
     */
    public static final String ANSI_RED = "\u001B[31m";
    /**
     * Constant <code>ANSI_GREEN="\u001B[32m"</code>
     */
    public static final String ANSI_GREEN = "\u001B[32m";
    /**
     * Constant <code>ANSI_YELLOW="\u001B[33m"</code>
     */
    public static final String ANSI_YELLOW = "\u001B[33m";
    /**
     * Constant <code>ANSI_BLUE="\u001B[34m"</code>
     */
    public static final String ANSI_BLUE = "\u001B[34m";
    /**
     * Constant <code>ANSI_PURPLE="\u001B[35m"</code>
     */
    public static final String ANSI_PURPLE = "\u001B[35m";
    /**
     * Constant <code>ANSI_CYAN="\u001B[36m"</code>
     */
    public static final String ANSI_CYAN = "\u001B[36m";
    /**
     * Constant <code>ANSI_WHITE="\u001B[37m"</code>
     */
    public static final String ANSI_WHITE = "\u001B[37m";


    public static String wrapANSI(String str, String ansiColor) {
        return ansiColor + str + ANSI_RESET;
    }

    /**
     * Resolve a path either in URI or local file form
     *
     * @param path
     * @return
     */
    public static Path resolvePath(String path) {
        if (path.matches("\\w:[\\\\/].*")) {
            return new File(path).toPath();
        } else {
            return Paths.get(URI.create(path));
        }
    }

    public static String[] parse(String line) {
        Scanner scan = new Scanner(line);
        ArrayList<String> tokens = new ArrayList<>();
        String token;
        Pattern pat = compile("[\"\'].*[\"\']");
        while (scan.hasNext()) {
            if (scan.hasNext("[\"\'].*")) {
                token = scan.findInLine(pat);
                if (token != null) {
                    token = token.substring(1, token.length() - 1);
                } else {
                    throw new RuntimeException("Syntax error.");
                }
            } else {
                token = scan.next();
            }
            tokens.add(token);
        }
        return tokens.toArray(new String[0]);

    }

    public static NavigableValuesSource readColumnedData(String fileName, String... names) throws FileNotFoundException {
        return readColumnedData(new File(fileName), names);
    }

    public static NavigableValuesSource readColumnedData(File file, String... names) throws FileNotFoundException {
        return readColumnedData(new FileInputStream(file));
    }

    public static NavigableValuesSource readColumnedData(InputStream stream, String... names) {
        ColumnedDataReader reader;
        if (names.length == 0) {
            reader = new ColumnedDataReader(stream);
        } else {
            reader = new ColumnedDataReader(stream, names);
        }
        ListTable.Builder res = new ListTable.Builder(names);
        for (Values dp : reader) {
            res.row(dp);
        }
        return res.build();
    }

    public static String formatCaption(TableFormat format) {
        return "#f " + format.getColumns()
                .map(columnFormat -> formatWidth(columnFormat.getName(), getDefaultTextWidth(columnFormat.getPrimaryType())))
                .collect(Collectors.joining("\t"));
    }

    public static String formatDataPoint(TableFormat format, Values dp) {
        return format.getColumns()
                .map(columnFormat -> format(dp.getValue(columnFormat.getName()), getDefaultTextWidth(columnFormat.getPrimaryType())))
                .collect(Collectors.joining("\t"));
    }

    public static File[] readFileMask(File workDir, String mask) {
        File dir;
        String newMask;
        //отрываем инфомацию о директории
        if (mask.contains(File.separator)) {
            int k = mask.lastIndexOf(File.separatorChar);
            dir = new File(workDir, mask.substring(0, k));
            newMask = mask.substring(k + 1);
        } else {
            dir = workDir;
            newMask = mask;
        }

        String regex = newMask.toLowerCase().replace(".", "\\.").replace("?", ".?").replace("*", ".+");
        return dir.listFiles(new RegexFilter(regex));
    }

    public static File getFile(File file, String path) {
        File f = new File(path);

        if (f.isAbsolute()) {
            return f;
        } else if (file.isDirectory()) {
            return new File(file, path);
        } else {
            return new File(file.getParentFile(), path);
        }
    }

    private static class RegexFilter implements FilenameFilter {

        String regex;

        public RegexFilter(String regex) {
            this.regex = regex;
        }

        @Override
        public boolean accept(File dir, String name) {
            return name.toLowerCase().matches(regex);
        }

    }

    /**
     * Get pre-defined default width for given value type
     *
     * @param type
     * @return
     */
    public static int getDefaultTextWidth(ValueType type) {
        switch (type) {
            case NUMBER:
                return 8;
            case BOOLEAN:
                return 6;
            case STRING:
                return 15;
            case TIME:
                return 20;
            case NULL:
                return 6;
            default:
                throw new AssertionError(type.name());
        }
    }

    public static String formatWidth(String val, int width) {
        if (width > 0) {
            return String.format("%" + width + "s", val);
        } else {
            return val;
        }
    }

    private static DecimalFormat getExpFormat(int width) {
        return new DecimalFormat(String.format("0.%sE0#;-0.%sE0#", grids(width - 6), grids(width - 7)));
    }

    private static String grids(int num) {
        if (num <= 0) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < num; i++) {
            b.append("#");
        }
        return b.toString();
    }

    private static String formatNumber(Number number, int width) {
        try {
            BigDecimal bd = new BigDecimal(number.toString());
//        if (number instanceof BigDecimal) {
//            bd = (BigDecimal) number;
//        } else if (number instanceof Integer) {
//            bd = BigDecimal.valueOf(number.getInt());
//        } else {
//
//            bd = BigDecimal.valueOf(number.doubleValue());
//        }

            if (bd.precision() - bd.scale() > 2 - width) {
                if (number instanceof Integer) {
                    return String.format("%d", number);
                } else {
                    return String.format("%." + (width - 1) + "g", bd.stripTrailingZeros());
                }
                //return getFlatFormat().format(bd);
            } else {
                return getExpFormat(width).format(bd);
            }
        } catch (Exception ex) {
            return number.toString();
        }
    }

    public static String format(Value val, int width) {
        switch (val.getType()) {
            case BOOLEAN:
                if (width >= 5) {
                    return Boolean.toString(val.getBoolean());
                } else if (val.getBoolean()) {
                    return formatWidth("+", width);
                } else {
                    return formatWidth("-", width);
                }
            case NULL:
                return formatWidth(NULL_STRING, width);
            case NUMBER:
                return formatWidth(formatNumber(val.getNumber(), width), width);
            case STRING:
                return formatWidth(val.getString(), width);
            case TIME:
                //TODO add time shortening
                return formatWidth(val.getString(), width);
            default:
                throw new IllegalArgumentException("Unsupported input value type");
        }
    }

    /**
     * Iterate over text lines in the input stream until new line satisfies the given condition.
     * The operation is non-buffering so after it, the stream position is at the end of stopping string.
     * The iteration stops when stream is exhausted.
     *
     * @param stream
     * @param charset       charset name for string encoding
     * @param stopCondition
     * @param action
     * @return the stop line (fist line that satisfies the stopping condition)
     */
    @Deprecated
    public static String forEachLine(InputStream stream, String charset, Predicate<String> stopCondition, Consumer<String> action) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (true) {
            try {
                int b = stream.read();
                if (b == -1) {
                    return baos.toString(charset).trim();
                }
                if (b == '\n') {
                    String line = baos.toString(charset).trim();
                    baos.reset();
                    if (stopCondition.test(line)) {
                        return line;
                    } else {
                        action.accept(line);
                    }
                } else {
                    baos.write(b);
                }
            } catch (IOException ex) {
                try {
                    return baos.toString(charset).trim();
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Return optional next line not fitting skip condition.
     *
     * @param stream
     * @param charset
     * @param skipCondition
     * @return
     */
    public static Optional<String> nextLine(InputStream stream, String charset, Predicate<String> skipCondition) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (true) {
            try {
                int b = stream.read();
                if (b == '\n') {
                    String line = baos.toString(charset).trim();
                    baos.reset();
                    if (!skipCondition.test(line)) {
                        return Optional.of(line);
                    }
                } else {
                    baos.write(b);
                }
            } catch (IOException ex) {
                return Optional.empty();
            }
        }
    }

    public static void writeString(DataOutput output, String string) throws IOException {
        byte[] bytes = string.getBytes(UTF8_CHARSET);
        output.writeShort(bytes.length);
        output.write(bytes);
    }

    public static String readString(DataInput input) throws IOException {
        int size = input.readShort();
        byte[] bytes = new byte[size];
        input.readFully(bytes);
        return new String(bytes, UTF8_CHARSET);
    }

}
