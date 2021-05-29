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

package hep.dataforge.data

import hep.dataforge.await
import hep.dataforge.context.Context
import hep.dataforge.context.FileReference
import hep.dataforge.data.binary.Binary
import hep.dataforge.goals.AbstractGoal
import hep.dataforge.goals.Coal
import hep.dataforge.goals.Goal
import hep.dataforge.goals.PipeGoal
import hep.dataforge.io.MetaFileReader
import hep.dataforge.io.envelopes.EnvelopeReader
import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import hep.dataforge.toList
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executor
import java.util.function.BiFunction
import java.util.function.Function
import java.util.stream.Stream

/**
 * Created by darksnake on 06-Sep-16.
 */
object DataUtils {

    const val META_DIRECTORY = "@meta"

    /**
     * Combine two data elements of different type into single data
     */
    fun <S1, S2, R> combine(context: Context,
                            data1: Data<out S1>, data2: Data<out S2>,
                            type: Class<R>,
                            meta: Meta,
                            transform: (S1, S2) -> R): Data<R> {
//        val combineGoal = object : AbstractGoal<R>() {
//            @Throws(Exception::class)
//            override fun compute(): R {
//                return transform(data1.get(), data2.get())
//            }
//
//            override fun dependencies(): Stream<Goal<*>> {
//                return Stream.of(data1.goal, data2.goal)
//            }
//        }
        val goal = Coal<R>(context, listOf(data1.goal, data2.goal)) {
            val res1 = data1.goal.await()
            val res2 = data2.goal.await()
            transform(res1,res2)
        }
        return Data(type, goal, meta)
    }


    /**
     * Join a uniform list of elements into a single datum
     */
    fun <R, S> join(data: Collection<Data<out S>>,
                    type: Class<R>,
                    meta: Meta,
                    transform: Function<List<S>, R>): Data<R> {
        val combineGoal = object : AbstractGoal<R>() {
            @Throws(Exception::class)
            override fun compute(): R {
                return transform.apply(data.stream().map { it.get() }.toList())
            }

            override fun dependencies(): Stream<Goal<*>> {
                return data.stream().map { it.goal }
            }
        }
        return Data(type, combineGoal, meta)
    }

    fun <R, S : Any> join(dataNode: DataNode<S>, type: Class<R>, transform: Function<List<S>, R>): Data<R> {
        val combineGoal = object : AbstractGoal<R>() {
            @Throws(Exception::class)
            override fun compute(): R {
                return transform.apply(dataNode.dataStream()
                        .filter { it.isValid }
                        .map { it.get() }
                        .toList()
                )
            }

            override fun dependencies(): Stream<Goal<*>> {
                return dataNode.dataStream().map { it.goal }
            }
        }
        return Data(type, combineGoal, dataNode.meta)
    }

    /**
     * Apply lazy transformation of the data using default executor. The meta of the result is the same as meta of input
     *
     * @param target
     * @param transformation
     * @param <R>
     * @return
     */
    fun <T, R> transform(data: Data<T>, target: Class<R>, transformation: (T) -> R): Data<R> {
        val goal = PipeGoal(data.goal, transformation)
        return Data(target, goal, data.meta)
    }

    fun <T, R> transform(data: Data<T>, target: Class<R>, executor: Executor, transformation: (T) -> R): Data<R> {
        val goal = PipeGoal(executor, data.goal, Function(transformation))
        return Data(target, goal, data.meta)
    }

    fun <T, R> transform(data: NamedData<T>, target: Class<R>, transformation: (T) -> R): NamedData<R> {
        val goal = PipeGoal(data.goal, transformation)
        return NamedData(data.name, target, goal, data.meta)
    }

    /**
     * A node containing single data fragment
     *
     * @param nodeName
     * @param data
     * @param <T>
     * @return
     */
    fun <T : Any> singletonNode(nodeName: String, data: Data<T>): DataNode<T> {
        return DataSet.edit(data.type)
                .apply { putData(DataNode.DEFAULT_DATA_FRAGMENT_NAME, data) }
                .build()
    }

    fun <T : Any> singletonNode(nodeName: String, `object`: T): DataNode<T> {
        return singletonNode(nodeName, Data.buildStatic(`object`))
    }

    /**
     * Reslove external meta for file if it is present
     */
    fun readExternalMeta(file: FileReference): Meta? {
        val metaFileDirectory = file.absolutePath.resolveSibling(META_DIRECTORY)
        return MetaFileReader.resolve(metaFileDirectory, file.absolutePath.fileName.toString()).orElse(null)
    }

    /**
     * Read an object from a file using given transformation. Capture a file meta from default directory. Override meta is placed above file meta.
     *
     * @param file
     * @param override
     * @param type
     * @param reader
     * @param <T>
     * @return
     */
    fun <T> readFile(file: FileReference, override: Meta, type: Class<T>, reader: (Binary) -> T): Data<T> {
        val filePath = file.absolutePath
        if (!Files.isRegularFile(filePath)) {
            throw IllegalArgumentException(filePath.toString() + " is not existing file")
        }
        val binary = file.binary
        val fileMeta = readExternalMeta(file)
        val meta = Laminate(fileMeta, override)
        return Data.generate(type, meta) { reader(binary) }
    }

    /**
     * Read file as Binary Data.
     *
     * @param file
     * @param override
     * @return
     */
    fun readFile(file: FileReference, override: Meta): Data<Binary> {
        return readFile(file, override, Binary::class.java) { it }
    }


    /**
     * Transform envelope file into data using given transformation. The meta of the data consists of 3 layers:
     *
     *  1. override - dynamic meta from method argument)
     *  1. captured - captured from @meta directory
     *  1. own - envelope owm meta
     *
     *
     * @param filePath
     * @param override
     * @param type
     * @param reader   a bifunction taking the binary itself and combined meta as arguments and returning
     * @param <T>
     * @return
     */
    fun <T> readEnvelope(filePath: Path, override: Meta, type: Class<T>, reader: BiFunction<Binary, Meta, T>): Data<T> {
        try {
            val envelope = EnvelopeReader.readFile(filePath)
            val binary = envelope.data
            val metaFileDirectory = filePath.resolveSibling(META_DIRECTORY)
            val fileMeta = MetaFileReader.resolve(metaFileDirectory, filePath.fileName.toString()).orElse(Meta.empty())
            val meta = Laminate(fileMeta, override, envelope.meta)
            return Data.generate(type, meta) { reader.apply(binary, meta) }
        } catch (e: IOException) {
            throw RuntimeException("Failed to read " + filePath.toString() + " as an envelope", e)
        }

    }
}
