///*
// * Copyright  2018 Alexander Nozik.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// *  Unless required by applicable law or agreed to in writing, software
// *  distributed under the License is distributed on an "AS IS" BASIS,
// *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *  See the License for the specific language governing permissions and
// *  limitations under the License.
// */
//
package hep.dataforge.states

//import hep.dataforge.description.NodeDef
//import hep.dataforge.description.ValueDef
//import java.lang.annotation.Inherited
//
///**
// * The definition of state for a stateful object.
// *
// * @property value The definition for state value
// * @property readable This state could be read
// * @property writable This state could be written
// * @author Alexander Nozik
// */
//@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
//@MustBeDocumented
//@Inherited
//@Repeatable
//annotation class MetaStateDef(
//        val value: NodeDef,
//        val readable: Boolean = true,
//        val writable: Boolean = false
//)
//
///**
// *
// * @author Alexander Nozik
// */
//@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
//@MustBeDocumented
//@Inherited
//annotation class MetaStateDefs(vararg val value: MetaStateDef)
//
///**
// * The definition of state for a stateful object.
// *
// * @property value The definition for state value
// * @property readable This state could be read
// * @property writable This state could be written
// * @author Alexander Nozik
// */
//@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
//@MustBeDocumented
//@Inherited
//@Repeatable
//annotation class StateDef(
//        val value: ValueDef,
//        val readable: Boolean = true,
//        val writable: Boolean = false
//)
//
///**
// *
// * @author Alexander Nozik
// */
//@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
//@MustBeDocumented
//@Inherited
//annotation class StateDefs(vararg val value: StateDef)