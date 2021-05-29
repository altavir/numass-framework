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

package hep.dataforge.maths.extensions

import org.apache.commons.math3.RealFieldElement


operator fun <T : RealFieldElement<T>> T.plus(arg: T): T = this.add(arg)

operator fun <T : RealFieldElement<T>> T.minus(arg: T): T = this.subtract(arg)

operator fun <T : RealFieldElement<T>> T.div(arg: T): T = this.divide(arg)

operator fun <T : RealFieldElement<T>> T.times(arg: T): T = this.multiply(arg)

operator fun <T : RealFieldElement<T>> T.plus(arg: Number): T = this.add(arg.toDouble())

operator fun <T : RealFieldElement<T>> T.minus(arg: Number): T = this.subtract(arg.toDouble())

operator fun <T : RealFieldElement<T>> T.div(arg: Number): T = this.divide(arg.toDouble())

operator fun <T : RealFieldElement<T>> T.times(arg: Number): T = this.multiply(arg.toDouble())

operator fun <T : RealFieldElement<T>> T.unaryMinus(): T  = this.negate()


fun <T : RealFieldElement<T>> abs(arg: T): T = arg.abs()

fun <T : RealFieldElement<T>> ceil(arg: T): T = arg.ceil()

fun <T : RealFieldElement<T>> floor(arg: T): T = arg.floor()

fun <T : RealFieldElement<T>> rint(arg: T): T = arg.rint()

fun <T : RealFieldElement<T>> sin(arg: T): T = arg.sin()

fun <T : RealFieldElement<T>> sqrt(arg: T): T = arg.sqrt()

fun <T : RealFieldElement<T>> exp(arg: T): T = arg.exp()

//TODO add everything else