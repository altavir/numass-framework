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

package hep.dataforge.io.envelopes

import hep.dataforge.meta.MetaBuilder
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

class TaglessEnvelopeTest {
    private val envelope = EnvelopeBuilder()
        .meta(MetaBuilder()
            .putValue("myValue", 12)
        ).data("Всем привет!".toByteArray(Charset.forName("UTF-8")))

    private val envelopeType = TaglessEnvelopeType.INSTANCE

    @Test
    fun testWriteRead() {
        val baos = ByteArrayOutputStream()
        envelopeType.writer.write(baos, envelope)

        println(String(baos.toByteArray()))

        val bais = ByteArrayInputStream(baos.toByteArray())
        val restored = envelopeType.reader.read(bais)

        assertEquals("Всем привет!", String(restored.data.buffer.array(), Charsets.UTF_8))
    }

    @Test
    fun testShortForm() {
        val envString = "<meta myValue=\"12\"/>\n" +
                "#~DATA~#\n" +
                "Всем привет!"
        println(envString)
        val bais = ByteArrayInputStream(envString.toByteArray(charset("UTF-8")))
        val restored = envelopeType.reader.read(bais)

        assertEquals(12, restored.meta.getInt("myValue"))
        assertEquals("Всем привет!", String(restored.data.buffer.array(), Charsets.UTF_8))
    }
}