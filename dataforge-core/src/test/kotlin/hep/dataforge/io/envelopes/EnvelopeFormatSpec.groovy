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

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import spock.lang.Specification

/**
 * Created by darksnake on 25-Feb-17.
 */
class EnvelopeFormatSpec extends Specification {
    def "Test read/write"() {
        given:
        byte[] data = "This is my data".bytes
        Meta meta = new MetaBuilder().setValue("myValue", "This is my meta")
        Envelope envelope = new EnvelopeBuilder().setMeta(meta).setData(data).build()
        when:
        def baos = new ByteArrayOutputStream();
        new DefaultEnvelopeWriter().write(baos, envelope)
        byte[] reaArray = baos.toByteArray();
        println new String(reaArray)
        def bais = new ByteArrayInputStream(reaArray)
        Envelope res = new DefaultEnvelopeReader().read(bais)
        then:
        res.data.buffer.array() == data
    }
}
