/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.stat.parametric;

import hep.dataforge.names.NameList;
import hep.dataforge.names.NameSetContainer;


public abstract class AbstractParametricValue extends AbstractParametric implements ParametricValue {

    public AbstractParametricValue(NameList names) {
        super(names);
    }

    public AbstractParametricValue(String[] list) {
        super(list);
    }

    public AbstractParametricValue(NameSetContainer set) {
        super(set);
    }


}
