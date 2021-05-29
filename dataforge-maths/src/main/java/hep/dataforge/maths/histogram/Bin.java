package hep.dataforge.maths.histogram;

import hep.dataforge.maths.domains.Domain;
import hep.dataforge.names.NameList;
import hep.dataforge.names.NameSetContainer;
import hep.dataforge.values.Values;
import org.jetbrains.annotations.Nullable;

/**
 * Created by darksnake on 29-Jun-17.
 */
public interface Bin extends Domain, NameSetContainer {

    /**
     * Increment counter and return new value
     *
     * @return
     */
    long inc();

    /**
     * The number of counts in bin
     *
     * @return
     */
    long getCount();

    /**
     * Set the counter and return old value
     *
     * @param c
     * @return
     */
    long setCount(long c);

    long getBinID();

    @Nullable
    NameList getNames();

    void setNames(NameList names);

    /**
     * Get the description of this bin as a set of named values
     *
     * @return
     */
    Values describe();
}
