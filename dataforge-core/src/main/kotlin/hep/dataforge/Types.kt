package hep.dataforge

import java.lang.annotation.Inherited
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.jvmName

/**
 * A text label for internal DataForge type classification. Alternative for mime container type.
 *
 * The DataForge type notation presumes that type `A.B.C` is the subtype of `A.B`
 */
@MustBeDocumented
@Inherited
annotation class Type(val id: String)

/**
 * Utils to get type of classes and objects
 */
object Types {
    operator fun get(cl: KClass<*>): String {
        return cl.findAnnotation<Type>()?.id ?: cl.jvmName
    }

    operator fun get(obj: Any): String{
        return get(obj::class)
    }
}

