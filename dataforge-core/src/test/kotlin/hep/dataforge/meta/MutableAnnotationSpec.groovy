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
package hep.dataforge.meta

import hep.dataforge.names.Name
import hep.dataforge.values.Value
import org.jetbrains.annotations.NotNull
import spock.lang.Specification

/**
 *
 * @author Alexander Nozik
 */
class MutableAnnotationSpec extends Specification {
    Configuration testAnnotation;

    def setup() {
        testAnnotation = new Configuration(new MetaBuilder("test")
                .putNode(new MetaBuilder("child")
                .putValue("child_value", 88)
        )
                .putValue("my_value", 48)
                .putValue("other_value", "ёлка")
                .putValue("path.to.my_value", true)
        )
    }


    def "test MutableAnnotation Value observer"() {
        when:
        testAnnotation.addListener(new Observer());
        testAnnotation.putValue("some_new_value", 13.3)
        then:
        testAnnotation.hasValue("some_new_value")

    }

    def "test child Value observer"() {
        when:
        testAnnotation.addListener(new Observer());
        testAnnotation.getMeta("child").putValue("new_child_value", 89);
        then:
        testAnnotation.hasValue("child.new_child_value")
    }


    private class Observer implements ConfigChangeListener {
        void notifyValueChanged(@NotNull Name name, Value oldItem, Value newItem) {
            println "the value with name ${name} changed from ${oldItem} to ${newItem}"
        }

        @Override
        void notifyNodeChanged(@NotNull Name name, @NotNull List<? extends Meta> oldItem, @NotNull List<? extends Meta> newItem) {
            println "the element with name ${name} changed from ${oldItem} to ${newItem}"
        }


    }
}

