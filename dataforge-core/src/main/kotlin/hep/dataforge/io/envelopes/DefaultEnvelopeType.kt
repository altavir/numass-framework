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


/**
 * @author darksnake
 */
open class DefaultEnvelopeType : EnvelopeType {

    override val code: Int = DEFAULT_ENVELOPE_CODE

    override val name: String = DEFAULT_ENVELOPE_NAME

    override fun description(): String = "Standard envelope type. Meta and data end auto detection are not supported. Tag is mandatory."

    override fun getReader(properties: Map<String, String>): EnvelopeReader = DefaultEnvelopeReader.INSTANCE

    override fun getWriter(properties: Map<String, String>): EnvelopeWriter = DefaultEnvelopeWriter(this, MetaType.resolve(properties))


    /**
     * True if metadata length auto detection is allowed
     *
     * @return
     */
    open fun infiniteDataAllowed(): Boolean = false

    /**
     * True if data length auto detection is allowed
     *
     * @return
     */
    open fun infiniteMetaAllowed(): Boolean = false

    companion object {

        val INSTANCE = DefaultEnvelopeType()

        const val DEFAULT_ENVELOPE_CODE = 0x44463032
        const val DEFAULT_ENVELOPE_NAME = "default"

        /**
         * The set of symbols that separates tag from metadata and data
         */
        val SEPARATOR = byteArrayOf('\r'.toByte(), '\n'.toByte())
    }

}
