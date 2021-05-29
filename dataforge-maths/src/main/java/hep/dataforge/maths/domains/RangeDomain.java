package hep.dataforge.maths.domains;

public class RangeDomain extends UnivariateDomain {

    private final double a;
    private final double b;

    public RangeDomain(double a, double b) {
        if(a>b){
            throw new IllegalArgumentException("b should be larger than a");
        }
        this.a = a;
        this.b = b;
    }


    @Override
    public boolean contains(double d) {
        return d >= a && d <= b;
    }

    @Override
    public Double getLower() {
        return a;
    }

    @Override
    public Double getUpper() {
        return b;
    }

    @Override
    public double volume() {
        return b - a;
    }
}
