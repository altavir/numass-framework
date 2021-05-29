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
package hep.dataforge.grind

import hep.dataforge.io.XMLMetaWriter
import hep.dataforge.meta.Meta
import spock.lang.Specification

/**
 *
 * @author Alexander Nozik
 */
class GrindMetaBuilderSpec extends Specification {

    def "Check meta building"() {
        when:
        Meta root = new GrindMetaBuilder().root(someValue: "some text here") {
            childNode(childNodeValue: ["some", "other", "text", "here"], something: 18)
            childNode(childNodeValue: "some givverish text here", something: 398)
            otherChildNode {
                grandChildNode(a: 22, text: "fslfjsldfj")
            }
            for (int i = 0; i < 10; i++) {
                numberedNode(number: i)
            }
        }

        then:
        println new XMLMetaWriter().writeString(root)
        root.getInt("otherChildNode.grandChildNode.a") == 22
    }

    def "Check simple meta"() {
        when:
        Meta m = Grind.parseMeta("myMeta");
        then:
        m.name == "myMeta"
    }

    def "Check unary operations"() {
        when:
        Meta root = new GrindMetaBuilder().root(someValue: "some text here") {
            put a: 22
        }
        then:
        root.getInt("a") == 22
    }

}

