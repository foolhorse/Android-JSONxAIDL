package me.machao.jsonxaidl.library

import android.text.TextUtils
import android.util.Log
import java.lang.IllegalArgumentException
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * Date  2019/1/26
 * @author charliema
 */
internal class ClassManager {

    companion object {

        const val TAG = "ClassManager"

        val instance: ClassManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            ClassManager()
        }
    }


    private val classMap = ConcurrentHashMap<String, Class<*>>()

    private val methodMap = ConcurrentHashMap<Class<*>, Map<String, Method>>()

    private val id2ObjectMap = ConcurrentHashMap<String, Any>()

    private val objectMap = ConcurrentHashMap<String, Any>()

    fun putClass(clz: Class<*>) {
        classMap.putIfAbsent(clz.name, clz)
        putMethods(clz)
    }

    fun putMethods(clz: Class<*>) {
        val methodArr = clz.methods
        val fieldArr = clz.fields
        val map = ConcurrentHashMap<String, Method>()
        methodArr.forEach {
            map.putIfAbsent(it.name, it)
        }
        methodMap.putIfAbsent(clz, map)
    }

    fun getMethod(className: String, methodName: String): Method? {
        if (TextUtils.isEmpty(methodName)) {
            return null
        }
        val clz = classMap[className]
        val map = methodMap[clz]
        return map!![methodName]
    }

//    fun getObject(name: String): Any? {
//        return objectMap[name]
//    }

//    fun putObject(id: String?, name: String, obj: Any) {
//        objectMap[name] = obj
//        if (id != null) {
//            id2ObjectMap.putIfAbsent(id, obj)
//        }
//    }

    fun putObject(id: String?, obj: Any) {
        if (id == null) {
            Log.e(TAG, "putObject id == null")
        } else {
            id2ObjectMap.putIfAbsent(id, obj)
        }
    }

    fun getObject(id: String?): Any? {
        return if (id == null) {
            Log.e(TAG, "getObject id == null")
            null
        } else {
            id2ObjectMap[id]
        }
    }


    fun removeObject(id: String) {
        id2ObjectMap.remove(id)
    }

    fun getClassType(className: String): Class<*>? {
        if (TextUtils.isEmpty(className)) {
            return null
        }
        var clz = classMap[className]
        if (clz != null) {
            return clz
        }
        try {
            clz = Class.forName(className)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }

        return clz
    }

    @Throws(Exception::class)
    fun getConstructor(clazzName: String?, parameterTypes: Array<Class<*>>?): Constructor<*>? {
        return getConstructor(classMap[clazzName], parameterTypes)
    }

    @Throws(Exception::class)
    fun getConstructor(clazz: Class<*>?, parameterTypes: Array<Class<*>>?): Constructor<*>? {
        if (clazz == null) {
            return null
        }
        var result: Constructor<*>? = null
        for (constructor in clazz.constructors) {
            if (classAssignable(constructor.parameterTypes, parameterTypes ?: emptyArray())) {
                if (result != null) {
                    throw IllegalArgumentException("${clazz.name} has too many constructors whose parameter types match the required types.")
                } else {
                    result = constructor
                }
            }
        }
        if (result == null) {
            throw NoSuchMethodException("${clazz.name} do not have a constructor whose parameter types match the required types.")
        }
        return result
    }

    private fun classAssignable(classes1: Array<Class<*>>, classes2: Array<Class<*>>): Boolean {
        if (classes1.size != classes2.size) {
            return false
        }
        val length = classes1.size
        for (i in 0 until length) {
            if (classes2[i] == null) {
                continue
            }
            if (primitiveMatch(classes1[i], classes2[i])) {
                continue
            }
            if (!classes1[i].isAssignableFrom(classes2[i])) {
                return false
            }
        }
        return true
    }

    private fun primitiveMatch(class1: Class<*>, class2: Class<*>): Boolean {
        return if (!class1.isPrimitive && !class2.isPrimitive) {
            false
        } else if (class1 == class2) {
            // both class are primitive or both are class
            true
        } else if (class1.isPrimitive) {
            // one class is class ,another is primitive
            primitiveMatch(class2, class1)
            // now class1 is class, class2 is primitive
        } else if (class1 == Boolean::class.java && class2 == Boolean::class.javaPrimitiveType) {
            true
        } else if (class1 == Byte::class.java && class2 == Byte::class.javaPrimitiveType) {
            true
        } else if (class1 == Char::class.java && class2 == Char::class.javaPrimitiveType) {
            true
        } else if (class1 == Short::class.java && class2 == Short::class.javaPrimitiveType) {
            true
        } else if (class1 == Int::class.java && class2 == Int::class.javaPrimitiveType) {
            true
        } else if (class1 == Long::class.java && class2 == Long::class.javaPrimitiveType) {
            true
        } else if (class1 == Float::class.java && class2 == Float::class.javaPrimitiveType) {
            true
        } else if (class1 == Double::class.java && class2 == Double::class.javaPrimitiveType) {
            true
        } else if (class1 == Void::class.java && class2 == Void.TYPE) {
            true
        } else {
            false
        }
    }

    private fun gc() {

    }


}