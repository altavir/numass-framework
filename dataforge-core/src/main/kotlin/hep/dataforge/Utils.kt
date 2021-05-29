package hep.dataforge

import java.lang.reflect.AnnotatedElement
import java.util.*
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

inline val <T> Optional<T>?.nullable: T?
    get() = this?.orElse(null)

inline val <T> T?.optional: Optional<T>
    get() = Optional.ofNullable(this)

/**
 * To use instead of ?: for block operations
 */
inline fun <T> T?.orElse(sup: () -> T): T {
    return this?: sup.invoke()
}


//Annotations

fun <T : Annotation> AnnotatedElement.listAnnotations(type: Class<T>, searchSuper: Boolean = true): List<T> {
    if (this is Class<*>) {
        val res = ArrayList<T>()
        val array = getDeclaredAnnotationsByType(type)
        res.addAll(Arrays.asList(*array))
        if (searchSuper) {
            val superClass = this.superclass
            if (superClass != null) {
                res.addAll(superClass.listAnnotations(type, true))
            }
            for (cl in this.interfaces) {
                res.addAll(cl.listAnnotations(type, true))
            }
        }
        return res;
    } else {
        val array = getAnnotationsByType(type)
        return Arrays.asList(*array)
    }
}

fun <T : Annotation> KAnnotatedElement.listAnnotations(type: KClass<T>, searchSuper: Boolean = true): List<T> {
    return when {
        this is KClass<*> -> return this.java.listAnnotations(type.java, searchSuper)
        this is KFunction<*> -> return this.javaMethod?.listAnnotations(type.java, searchSuper) ?: emptyList()
        else -> {
            //TODO does not work for containers
            this.annotations.filterIsInstance(type.java)
        }

    }
}

inline fun <reified T : Annotation> KAnnotatedElement.listAnnotations(searchSuper: Boolean = true): List<T> {
    return listAnnotations(T::class, searchSuper)
}


//object IO {
//    /**
//     * Create an output stream that copies its output into each of given streams
//     */
//    fun mirrorOutput(vararg outs: OutputStream): OutputStream {
//        return object : OutputStream() {
//            override fun write(b: Int) = outs.forEach { it.write(b) }
//
//            override fun write(b: ByteArray?) = outs.forEach { it.write(b) }
//
//            override fun write(b: ByteArray?, off: Int, len: Int) = outs.forEach { it.write(b, off, len) }
//
//            override fun flush() = outs.forEach { it.flush() }
//
//            override fun close() = outs.forEach { it.close() }
//        }
//    }
//}