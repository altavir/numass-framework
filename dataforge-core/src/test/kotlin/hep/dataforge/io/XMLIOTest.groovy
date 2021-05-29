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

package hep.dataforge.io

import hep.dataforge.meta.MetaBuilder
import spock.lang.Specification

class XMLIOTest extends Specification {


    def "XML IO"() {
        given:
        def testMeta =
                new MetaBuilder("test")
                        .putValue("numeric", 22.5)
                        .putValue("other", "otherValue")
                        .putValue("some.path", true)
                        .putNode(
                        new MetaBuilder("child")
                                .putValue("childValue", "childValue")
                                .putNode(
                                new MetaBuilder("grandChild")
                                        .putValue("grandChildValue", "grandChildValue")
                        ))
                        .putNode(
                        new MetaBuilder("child")
                                .putValue("childValue", "otherChildValue")
                                .putNode(
                                new MetaBuilder("grandChild")
                                        .putValue("grandChildValue", "otherGrandChildValue")
                        )
                ).build();
        when:
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new XMLMetaWriter().write(baos, testMeta)
        def bytes = baos.toByteArray();
        def res = new XMLMetaReader().read(new ByteArrayInputStream(bytes))
        then:
        res == testMeta
    }

    def "XMlinput"(){
        given:
        def source = "<loader index=\"timestamp\" name=\"msp802596725\" type=\"point\">\n" +
                "    <format>\n" +
                "        <column name=\"timestamp\" type=\"TIME\"/>\n" +
                "        <column name=\"2\" type=\"NUMBER\"/>\n" +
                "        <column name=\"3\" type=\"NUMBER\"/>\n" +
                "        <column name=\"4\" type=\"NUMBER\"/>\n" +
                "        <column name=\"5\" type=\"NUMBER\"/>\n" +
                "        <column name=\"6\" type=\"NUMBER\"/>\n" +
                "        <column name=\"12\" type=\"NUMBER\"/>\n" +
                "        <column name=\"14\" type=\"NUMBER\"/>\n" +
                "        <column name=\"18\" type=\"NUMBER\"/>\n" +
                "        <column name=\"22\" type=\"NUMBER\"/>\n" +
                "        <column name=\"28\" type=\"NUMBER\"/>\n" +
                "        <column name=\"32\" type=\"NUMBER\"/>\n" +
                "    </format>\n" +
                "</loader>"
        when:
        def res = new XMLMetaReader().read(new ByteArrayInputStream(source.bytes))
        then:
        res.getInt("format.column[2].name") == 3
    }
}
