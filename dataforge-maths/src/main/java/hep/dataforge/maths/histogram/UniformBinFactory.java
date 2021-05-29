package hep.dataforge.maths.histogram;

import hep.dataforge.utils.ArgumentChecker;

/**
 * Create a uniform binning with given start point and steps
 */
public class UniformBinFactory implements BinFactory {
    private final Double[] binStart;
    private final Double[] binStep;

    public UniformBinFactory(Double[] binStart, Double[] binStep) {
        ArgumentChecker.checkEqualDimensions(binStart.length, binStep.length);
        this.binStart = binStart;
        this.binStep = binStep;
    }


    @Override
    public SquareBin createBin(Double... point) {
        ArgumentChecker.checkEqualDimensions(point.length, binStart.length);
        Double[] lo = new Double[point.length];
        Double[] up = new Double[point.length];

        for (int i = 0; i < point.length; i++) {
            if (point[i] > binStart[i]) {
                lo[i] = binStart[i] + Math.floor((point[i] - binStart[i]) / binStep[i]) * binStep[i];
                up[i] = lo[i] + binStep[i];
            } else {
                up[i] = binStart[i] - Math.floor((binStart[i] - point[i]) / binStep[i]) * binStep[i];
                lo[i] = up[i] - binStep[i];
            }
        }
        return new SquareBin(lo, up);
    }

    public int getDimension(){
        return binStart.length;
    }
}
