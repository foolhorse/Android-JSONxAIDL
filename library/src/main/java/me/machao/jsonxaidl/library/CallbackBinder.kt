package me.machao.jsonxaidl.library

import android.util.Log
import com.google.gson.Gson
import me.machao.jsonxaidl.library.model.Request
import java.lang.reflect.InvocationTargetException

/**
 * Date  2019/2/12
 * @author charliema
 */
class CallbackBinder : IPCCallbackAidlInterface.Stub() {

    companion object {
        const val TAG = "CallbackBinder"
    }

    override fun callback(requestStr: String?): String? {
        Log.e(TAG, "callback in caller process:$requestStr")
        val request = Gson().fromJson(requestStr, Request::class.java)

        val obj = ClassManager.instance.getObject(request.objId!!)

        var clz: Class<*>? = null
        if (obj != null) {
            clz = obj.javaClass
        } else {
            return null
        }

        val method = clz.getMethod(request.methodName!!)

//            val parameterObjArr = generateParameterObjectArray(request)

        var returnObj: Any? = null
        try {
//                returnObj = if (parameterObjArr == null) {
//                    method!!.invoke(obj)
//                } else {
//                    method!!.invoke(obj, *parameterObjArr)
//                }
            method.invoke(obj)

        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }

        // TODO wrap response?
        return Gson().toJson(returnObj)
    }
}