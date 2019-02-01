package me.machao.jsonxaidl.library.util

import android.util.Log
import com.google.gson.Gson

/**
 * Date  2019/1/25
 * @author charliema
 */
object GsonExt {

    private val gson = Gson()

    fun Any.toJson(): String {
        return gson.toJson(this)
    }

}

fun Any.log(msg: String) {
    Log.d("_" + this::class.java.simpleName, msg)
}

