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
package inr.numass.scripts

import hep.dataforge.context.Context
import hep.dataforge.data.DataNode
import hep.dataforge.stat.fit.FitTaskResult
import inr.numass.Main
import inr.numass.Numass


//Main.main("-lc")
Context context = Numass.buildContext();
context.putValue("integralThreshold", 15d);
DataNode resultPack = Main.run(context, "-c","D:\\sterile-new\\loss2014-11\\d2_19_1.xml")
FitTaskResult result = resultPack.getData().get()
result.print(new PrintWriter(System.out))
