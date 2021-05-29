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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.io

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.io.envelopes.EnvelopeReader
import hep.dataforge.io.envelopes.MetaType
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.text.ParseException
import java.util.*

//TODO add examples for transformations

/**
 * A reader for meta file in any supported format. Additional file formats could
 * be statically registered by plug-ins.
 *
 * Basically reader performs two types of "on read" transformations:
 *
 * Includes: include a meta from given file instead of given node
 *
 * Substitutions: replaces all occurrences of `${<key>}` in child meta nodes by given value. Substitutions are made as strings.
 * @author [Alexander Nozik](mailto:altavir@gmail.com)
 */
class MetaFileReader {

    fun read(context: Context, path: String): Meta {
        return read(context, context.rootDir.resolve(path))
    }

    fun read(context: Context, file: Path): Meta {
        val fileName = file.fileName.toString()
        for (type in context.serviceStream(MetaType::class.java)) {
            if (type.fileNameFilter.invoke(fileName)) {
                return transform(context, type.reader.readFile(file))
            }
        }
        //Fall back and try to resolve meta as an envelope ignoring extension
        return EnvelopeReader.readFile(file).meta
    }

    /**
     * Evaluate parameter substitution and include substitution
     *
     * @param builder
     * @return
     */
    private fun transform(context: Context, builder: MetaBuilder): MetaBuilder {
        return builder.substituteValues(context)
    }

    private fun evaluateSubst(context: Context, subst: String): String {
        return subst
    }

    companion object {

        const val SUBST_ELEMENT = "df:subst"
        const val INCLUDE_ELEMENT = "df:include"

        private val instance = MetaFileReader()

        fun instance(): MetaFileReader {
            return instance
        }

        fun read(file: Path): Meta {
            try {
                return instance().read(Global, file)
            } catch (e: IOException) {
                throw RuntimeException("Failed to read meta file " + file.toString(), e)
            } catch (e: ParseException) {
                throw RuntimeException("Failed to read meta file " + file.toString(), e)
            }

        }

        /**
         * Resolve the file with given name (without extension) in the directory and read it as meta. If multiple files with the same name exist in the directory, the ran
         *
         * @param directory
         * @param name
         * @return
         */
        fun resolve(directory: Path, name: String): Optional<Meta> {
            try {
                return Files.list(directory).filter { it -> it.startsWith(name) }.findFirst().map{ read(it) }
            } catch (e: IOException) {
                throw RuntimeException("Failed to list files in the directory " + directory.toString(), e)
            }

        }
    }

}
