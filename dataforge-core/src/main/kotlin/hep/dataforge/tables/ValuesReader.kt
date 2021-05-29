/*
 * Copyright  2018 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package hep.dataforge.tables

import hep.dataforge.io.LineIterator
import hep.dataforge.values.LateParseValue
import hep.dataforge.values.ValueMap
import hep.dataforge.values.Values
import java.io.InputStream

/**
 *
 * ValuesParser interface.
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
interface ValuesParser {
    /**
     *
     * parse.
     *
     * @param str a [java.lang.String] object.
     * @return
     */
    fun parse(str: String): Values
}


/**
 *
 *
 * SimpleValuesParser class.
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
class SimpleValuesParser : ValuesParser {

    private val format: Array<String>

    /**
     *
     *
     * Constructor for SimpleDataParser.
     *
     * @param format an array of [java.lang.String] objects.
     */
    constructor(format: Array<String>) {
        this.format = format
    }

    /**
     * Создаем парсер по заголовной строке
     *
     * @param line a [java.lang.String] object.
     */
    constructor(line: String) {
        this.format = line.trim { it <= ' ' }.split("[^\\w']*".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    }

    constructor(format: TableFormat) {
        this.format = format.namesAsArray()
    }

    /**
     * {@inheritDoc}
     *
     * @param str
     */
    override fun parse(str: String): Values {
        val strings = str.split("\\s".toRegex())
        return ValueMap((0 until format.size).associate { format[it] to LateParseValue(strings[it]) })
    }

}


/**
 *
 * Считаем, что формат файла следующий: сначала идут метаданные, потом данные
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
class ValuesReader(private val reader: Iterator<String>, private val parser: ValuesParser) : Iterator<Values> {

    @Volatile
    var pos = 0
        private set

    constructor(stream: InputStream, parser: ValuesParser) : this(LineIterator(stream), parser)

    constructor(stream: InputStream, names: Array<String>) : this(LineIterator(stream), SimpleValuesParser(names))

    constructor(reader: Iterator<String>, names: Array<String>) : this(reader, SimpleValuesParser(names))

    constructor(reader: Iterator<String>, headline: String) : this(reader, SimpleValuesParser(headline))

    /**
     * {@inheritDoc}
     *
     * @return
     */
    override fun hasNext(): Boolean {
        return reader.hasNext()
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    override fun next(): Values {
        return parser.parse(reader.next()).also {
            pos++
        }
    }

    fun skip(n: Int) {
        for (i in 0 until n) {
            reader.next()
            pos++
        }
    }

}
