package inr.numass.models.misc

import hep.dataforge.stat.parametric.AbstractParametricFunction
import hep.dataforge.values.Values

class ConstantTail(private val defaultTail: Double = 0.0) : AbstractParametricFunction("tail") {
    override fun derivValue(parName: String, x: Double, set: Values): Double {
        if (parName == "tail" && x <= 0) {
            return 1.0
        } else {
            return 0.0
        }
    }

    override fun value(x: Double, set: Values): Double {
        return if (x <= 0) {
            set.getDouble("tail", defaultTail)
        } else {
            0.0
        }
    }

    override fun providesDeriv(name: String): Boolean {
        return true;
    }
}

class exponentialTail : AbstractParametricFunction("tail") {
    override fun derivValue(parName: String?, x: Double, set: Values?): Double {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun value(x: Double, set: Values?): Double {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun providesDeriv(name: String?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}