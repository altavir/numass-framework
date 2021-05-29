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

package hep.dataforge.values

import spock.lang.Specification

import java.time.Instant

/**
 * Created by darksnake on 02-Oct-16.
 */
class ValueUtilsTest extends Specification {
    def "IsBetween"() {
        expect:
        ValueUtils.isBetween(0.5, Value.of(false), Value.of(true));
    }

    def "ValueIO"() {
        given:

        def timeValue = Value.of(Instant.now());
        def stringValue = Value.of("The string с русскими буквами");
        def listValue = Value.of(1d, 2d, 3d);
        def booleanValue = Value.of(true);
        def numberValue = Value.of(BigDecimal.valueOf(22.5d));
        when:

        //writing values
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        ValueUtils.writeValue(oos, timeValue);
        ValueUtils.writeValue(oos, stringValue);
        ValueUtils.writeValue(oos, listValue);
        ValueUtils.writeValue(oos, booleanValue);
        ValueUtils.writeValue(oos, numberValue);

        oos.flush()
        byte[] bytes = baos.toByteArray();
        println "Serialized preview: \t" + new String(bytes)

        //reading values
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);

        then:

        ValueUtils.readValue(ois) == timeValue;
        ValueUtils.readValue(ois) == stringValue;
        ValueUtils.readValue(ois) == listValue;
        ValueUtils.readValue(ois) == booleanValue;
        ValueUtils.readValue(ois) == numberValue;

    }
}
