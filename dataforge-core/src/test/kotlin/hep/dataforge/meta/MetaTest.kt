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

import hep.dataforge.exceptions.NamingException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 *
 * @author darksnake
 */
class MetaTest {

    internal val testAnnotation = MetaBuilder("test")
            .putValue("some", "\${other}")
            .putValue("numeric", 22.5)
            .putValue("other", "otherValue")
            .putValue("some.path", true)
            .putNode(MetaBuilder("child")
                    .putValue("childValue", "childValue")
                    .putNode(MetaBuilder("grandChild")
                            .putValue("grandChildValue", "grandChildValue")
                    )
            )
            .putNode(MetaBuilder("child")
                    .putValue("childValue", "otherChildValue")
                    .putNode(MetaBuilder("grandChild")
                            .putValue("grandChildValue", "otherGrandChildValue")
                    )
            )
            .build()

    @Before
    fun setUp() {

    }

    @After
    fun tearDown() {
    }

    //    @Test
    //    public void testSubst() {
    //        System.onComplete.println("Value substitution via AnnotationReader");
    //        assertEquals("otherValue", testAnnotation.getString("some"));
    //    }

    @Test
    fun testPath() {
        println("Path search")
        assertEquals("childValue", testAnnotation.getString("child.childValue"))
        assertEquals("grandChildValue", testAnnotation.getString("child.grandChild.grandChildValue"))
        assertEquals("otherGrandChildValue", testAnnotation.getString("child[1].grandChild.grandChildValue"))
    }

    @Test(expected = NamingException::class)
    fun testWrongPath() {
        println("Missing path search")
        assertEquals("otherGrandChildValue", testAnnotation.getString("child[2].grandChild.grandChildValue"))
    }
}
