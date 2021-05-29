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
package hep.dataforge.stat.fit;

import hep.dataforge.names.NameList;
import hep.dataforge.values.Value;
import hep.dataforge.values.ValueFactory;
import kotlin.Pair;

import java.io.PrintWriter;

/**
 * Контейнер для несимметричных оценок и доверительных интервалов
 *
 * @author Darksnake
 * @version $Id: $Id
 */
public class MINOSResult implements IntervalEstimate {

    private double[] errl;
    private double[] errp;
    private String[] names;


    /**
     * <p>Constructor for MINOSResult.</p>
     *
     * @param list an array of {@link java.lang.String} objects.
     */
    public MINOSResult(String[] list, double[] errl, double[] errp) {
        this.names = list;
        this.errl = errl;
        this.errp = errp;
    }

    @Override
    public NameList getNames() {
        return new NameList(names);
    }

    @Override
    public Pair<Value, Value> getInterval(String parName) {
        int index = getNames().getNumberByName(parName);
        return new Pair<>(ValueFactory.of(errl[index]), ValueFactory.of(errp[index]));
    }

    @Override
    public double getCL() {
        return 0.68;
    }

    /** {@inheritDoc} */
    @Override
    public void print(PrintWriter out) {
        if ((this.errl != null) || (this.errp != null)) {
            out.println();
            out.println("Assymetrical errors:");
            out.println();
            out.println("Name\tLower\tUpper");
            for (int i = 0; i < this.getNames().size(); i++) {
                out.print(this.getNames().get(i));
                out.print("\t");
                if (this.errl != null) {
                    out.print(this.errl[i]);
                } else {
                    out.print("---");
                }
                out.print("\t");
                if (this.errp != null) {
                    out.print(this.errp[i]);
                } else {
                    out.print("---");
                }
                out.println();
            }
        }
    }

}
