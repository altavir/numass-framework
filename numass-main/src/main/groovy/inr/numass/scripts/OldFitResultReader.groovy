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

import hep.dataforge.datafitter.ParamSet
import hep.dataforge.maths.NamedDoubleArray
import hep.dataforge.maths.NamedDoubleSet
import hep.dataforge.maths.NamedMatrix
import java.util.regex.Pattern
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.RealMatrix
import java.util.Scanner
import java.util.regex.Matcher

/**
 *
 * @author darksnake
 */
class OldFitResultReader {
    private static Pattern meanPattern = ~/\"(?<name>.*)\"\s*:\s*(?<value>[\d.]*)\s*/;
    
    private NamedDoubleSet means;
    private RealMatrix covariance;
    
    void readMeans(String input){
        Scanner scan = new Scanner(input);
        List<String> names = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        while(scan.hasNextLine()){
            String nextLine = scan.nextLine();
            if(!nextLine.isEmpty()){
                Matcher match = meanPattern.matcher(nextLine);
                match.matches();
                names << match.group("name");
                values << Double.parseDouble(match.group("value"));
            }
        }
        this.means = new NamedDoubleArray(names.toArray(new String[names.size()]), values.toArray(new double[values.size()]));
    }
    
    void readCovariance(String input){
        Scanner scan = new Scanner(input);
        List<List<Double>> matrix = new ArrayList<>();
        while(scan.hasNextLine()){
            String nextLine = scan.nextLine();
            if(!nextLine.isEmpty()){
                Scanner lineScan = new Scanner(nextLine);
                List<Double> line = new ArrayList<>();
                while(lineScan.hasNextDouble()){
                    line << lineScan.nextDouble();
                }
                matrix << line;
            }
        }
        
        Array2DRowRealMatrix result = new Array2DRowRealMatrix(matrix.size(),matrix.size());
        for(int i = 0; i< matrix.size; i++){
            List<Double> line = matrix.get(i);
            for(int j = 0; j< matrix.size; j++){
                result.setEntry(i,j,line[j]);
            }
        }
        this.covariance = result;
    }
    
    void readFile(File file){
        String text = file.getText();
        Pattern pattern = Pattern.compile(/.*The best fit values are:\s*(?<parameters>.*)Covariation marix:\s*(?<covariance>.*)Best Likelihood logarithm/, Pattern.DOTALL);
        List results = text.findAll(pattern);
       
        String res = results.get(results.size()-1);
        Matcher match = pattern.matcher(res);
        match.matches();
        readMeans(match.group("parameters"));
        readCovariance(match.group("covariance"));
    }
    
    
    NamedDoubleSet getMeans(){
        return means;
    }
    
    ParamSet getParamSet(){
        ParamSet res = new ParamSet();
        NamedMatrix cov = getCovariance();
        for(String name: means.names()){
            res.setPar(name, means.getValue(name), Math.sqrt(Math.abs(cov.getElement(name, name))));
        }
        return res;
    }
    
    NamedMatrix getCovariance(){
        return new NamedMatrix(covariance, means.namesAsArray());
    }
    
}

