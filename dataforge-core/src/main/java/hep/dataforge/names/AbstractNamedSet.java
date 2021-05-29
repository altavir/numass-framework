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
package hep.dataforge.names;

/**
 * <p>AbstractNamedSet class.</p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class AbstractNamedSet implements NameSetContainer {
    
    private final NameList names;

    /**
     * <p>Constructor for AbstractNamedSet.</p>
     *
     * @param names a {@link hep.dataforge.names.NameList} object.
     */
    public AbstractNamedSet(NameList names) {
        this.names = names;
    }
    
    /**
     * <p>Constructor for AbstractNamedSet.</p>
     *
     * @param list an array of {@link java.lang.String} objects.
     */
    public AbstractNamedSet(String[] list) {
        this.names = new NameList(list);
    }

    /**
     * <p>Constructor for AbstractNamedSet.</p>
     *
     * @param set a {@link hep.dataforge.names.NameSetContainer} object.
     */
    public AbstractNamedSet(NameSetContainer set) {
        this.names = set.getNames();
    }    
    
    /** {@inheritDoc} */
    @Override
    public NameList getNames() {
        return names;
    }
    
}
