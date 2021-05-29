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

package hep.dataforge.storage

import hep.dataforge.context.Global
import hep.dataforge.storage.files.TableLoaderType
import hep.dataforge.tables.MetaTableFormat
import hep.dataforge.values.ValueMap
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant


class TableLoaderTest {
    val tableLoaderType = TableLoaderType()

    companion object {
        lateinit var dir: Path

        @BeforeClass
        @JvmStatic
        fun setup() {
            dir = Files.createTempDirectory(Global.tmpDir, "storage-test")
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            dir.toFile().deleteRecursively()
        }
    }

    fun benchmark(action: () -> Unit): Duration {
        val start = Instant.now()
        action.invoke()
        return Duration.between(start, Instant.now())
    }

    @Test
    fun testReadWrite() {
        val path = dir.resolve("read-write.df")
        val format = MetaTableFormat.forNames("a", "b", "c")
        val loader = tableLoaderType.create(Global, path, format)
        val writer = loader.mutable()

        runBlocking {
            (1..10).forEach {
                writer.append(it, it + 1, it * 2)
            }
        }

        assertEquals(3, runBlocking { loader.get(1)?.getInt("b")})
        writer.close()
        loader.close()
    }

    @Test
    fun testPerformance() {
        val n = 10000

        val path = dir.resolve("performance.df")
        val format = MetaTableFormat.forNames("a", "b", "c")
        val loader = tableLoaderType.create(Global, path, format)
        val writer = loader.mutable()
        val data = (1..n).map { ValueMap.of(format.namesAsArray(), it, it + 1, it * 2) }
        val writeTime = benchmark {
            writer.appendAll(data)
        }
        println("Write of $n elements completed in $writeTime. The average time per element is ${writeTime.toMillis().toDouble() / n} milliseconds")
        writer.close()
        loader.close()

        val reader = tableLoaderType.read(Global, path)

        var lastValue: Int
        val readTime = benchmark {
            reader.forEachIndexed { index, it ->
                lastValue = it["a"].int
                if (lastValue != index + 1) {
                    throw error("Data read broken on element $index")
                }
            }
        }

        assertEquals(n - 1, reader.keys.last().int)
        println("Read of $n elements completed in $readTime. The average time per element is ${readTime.toMillis().toDouble() / n} milliseconds")
        assert(writeTime < Duration.ofMillis((0.05*n).toLong()))
        assert(readTime < Duration.ofMillis((0.01*n).toLong()))
    }
}