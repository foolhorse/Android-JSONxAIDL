package me.machao.jsonxaidl.library.util

import android.util.Log
import com.google.gson.GsonBuilder

/**
 * Date  2019/1/25
 * @author charliema
 */
object GsonExt {

    private val gson = GsonBuilder()
        .registerTypeAdapterFactory(ClassTypeAdapterFactory())
        .create()

    fun Any.toJson(): String {
        return gson.toJson(this)
    }

    fun <T> String.fromJson(clz: Class<*>): T {
        return gson.fromJson(this, clz) as T
    }

}

fun Any.log(msg: String) {
    Log.d("_" + this::class.java.simpleName, msg)
}

