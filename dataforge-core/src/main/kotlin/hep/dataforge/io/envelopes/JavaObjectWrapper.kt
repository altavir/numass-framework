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
package hep.dataforge.io.envelopes

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * @author Alexander Nozik
 */
object JavaObjectWrapper : Wrapper<Any> {
    const val JAVA_CLASS_KEY = "javaClass"
    const val JAVA_OBJECT_TYPE = "hep.dataforge.java"
    const val JAVA_SERIAL_DATA = "java.serial"

    override val type: Class<Any>
        get() = Any::class.java

    override val name: String = JAVA_OBJECT_TYPE

    override fun wrap(obj: Any): Envelope {
        val builder = EnvelopeBuilder()
                .setDataType(JAVA_SERIAL_DATA)
                .setEnvelopeType(JAVA_OBJECT_TYPE)
                .setMetaValue(JAVA_CLASS_KEY, obj.javaClass.name)
        val baos = ByteArrayOutputStream()
        try {
            ObjectOutputStream(baos).use { stream ->
                stream.writeObject(obj)
                builder.data(baos.toByteArray())
                return builder.build()
            }
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }

    }

    override fun unWrap(envelope: Envelope): Any {
        if (name != envelope.type) {
            throw Error("Wrong envelope type: " + envelope.type)
        }
        try {
            val stream = ObjectInputStream(envelope.data.stream)
            return stream.readObject()
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        } catch (ex: ClassNotFoundException) {
            throw RuntimeException(ex)
        }

    }


}
