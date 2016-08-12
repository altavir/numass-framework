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

import org.apache.commons.io.FilenameUtils

import static groovy.io.FileType.FILES


File dir = new File("D:\\loss-2014\\");
File resultDir = new File(dir, ".dataforge\\showLoss\\");
if(!resultDir.exists()){
    resultDir.mkdirs()
}

File resultFile = new File(resultDir,"summary");


resultFile.setText("name\tX\tX_err\texPos\texPos_err\tionPos\tionPos_err\texW\texW_err\tionW\tionW_err\texIonRatio\texIonRatio_err\tionRatio\tionRatioErr\tchi\r\n");

dir.eachFileMatch FILES, {it ==~ /[dh]2_\d\d_\d(?:_bkg)?\.xml/}, {
    try{
        inr.numass.Main.main("-c", it.getAbsolutePath())
        File outFile = new File(resultDir, FilenameUtils.getBaseName(it.getName()) + "_loss.onComplete")
        resultFile.append(outFile.readLines().get(50));
        resultFile.append("\r\n");
    } catch(Exception ex){
        ex.printStackTrace();
    }
}