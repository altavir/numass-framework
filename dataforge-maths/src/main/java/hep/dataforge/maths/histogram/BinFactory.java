package hep.dataforge.maths.histogram;

/**
 * Creates a new bin with zero count corresponding to given point
 */
public interface BinFactory {
    Bin createBin(Double... point);
}
