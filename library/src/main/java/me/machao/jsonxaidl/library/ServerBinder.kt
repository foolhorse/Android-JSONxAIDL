package me.machao.jsonxaidl.library

import android.os.IBinder
import android.os.Process
import android.os.RemoteException
import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import me.machao.jsonxaidl.library.model.Request
import me.machao.jsonxaidl.library.model.RequestParameter
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/**
 * Date  2019/2/12
 * @author charliema
 */
class ServerBinder : IPCAidlInterface.Stub() {

    companion object {
        const val TAG = "ServerBinder "
    }

    private val callbackInterfaceMap = ConcurrentHashMap<Int, IPCCallbackAidlInterface>()


    @Throws(RemoteException::class)
    override fun call(requestStr: String): String? {
        try {
            val request = Gson().fromJson(requestStr, Request::class.java)
            Log.e(TAG, "GET_INSTANCE: request:" + request)

            when (request.type) {
                Request.GET_SINGLETON_INSTANCE -> {

                    val getInstanceMethod = ClassManager.instance
                        .getMethod(request.className!!, "getInstance")

                    val parameterObjArr = generateParameterObjectArray(request)

                    var obj: Any? = null

                    try {
                        obj = if (parameterObjArr == null) {
                            getInstanceMethod!!.invoke(null)
                        } else {
                            getInstanceMethod!!.invoke(null, *parameterObjArr)
                        }
                        ClassManager.instance.putObject(request.objId, obj!!)
                        // TODO wrap response?
                        return Gson().toJson(obj)
                    } catch (e: IllegalAccessException) {
                        e.printStackTrace()
                    } catch (e: InvocationTargetException) {
                        e.printStackTrace()
                    }
                }
                Request.NEW_INSTANCE -> {
                    val parameterTypeArr = generateParameterTypeArray(request.requestParameters)

                    val constructorMethod = ClassManager.instance
                        .getConstructor(request.className, parameterTypeArr)

                    if (constructorMethod == null) {
                        throw RemoteException("NEW_INSTANCE, can't find Constructor")
                    }

                    val parameterObjArr = generateParameterObjectArray(request)

                    var obj: Any? = null

                    try {
                        obj = if (parameterObjArr == null) {
                            constructorMethod.newInstance()
                        } else {
                            constructorMethod.newInstance(*parameterObjArr)
                        }
                        ClassManager.instance.putObject(request.objId, obj!!)
                        // TODO wrap response?
                        return Gson().toJson(obj)
                    } catch (e: IllegalAccessException) {
                        e.printStackTrace()
                    } catch (e: InvocationTargetException) {
                        e.printStackTrace()
                    }
                }
                Request.INVOKE_METHOD -> {
                    val obj = ClassManager.instance.getObject(request.objId!!)
                    val method = ClassManager.instance.getMethod(request.className!!, request.methodName!!)

                    val parameterObjArr = generateParameterObjectArray(request)

                    var returnObj: Any? = null
                    try {
                        returnObj = if (parameterObjArr == null) {
                            method!!.invoke(obj)
                        } else {
                            method!!.invoke(obj, *parameterObjArr)
                        }
                    } catch (e: IllegalAccessException) {
                        e.printStackTrace()
                    } catch (e: InvocationTargetException) {
                        e.printStackTrace()
                    }

                    // TODO wrap response?
                    return Gson().toJson(returnObj)
                }
                else -> {
                }
            }
            return ""
        } catch (e: Exception) {
            e.printStackTrace()
            // TODO RemoteException
            throw e as RemoteException
        } finally {

        }
    }

    override fun setCallbackIInterface(pid: Int, iBinder: IBinder?) {
        val ipcCallbackIInterface = IPCCallbackAidlInterface.Stub.asInterface(iBinder)
        callbackInterfaceMap.putIfAbsent(pid, ipcCallbackIInterface)
    }

    override fun gc(objIdList: MutableList<String>?) {
        Log.e(TAG, "gc in server process")
        objIdList?.forEach {
            ClassManager.instance.removeObject(it)
        }
    }


    private fun generateParameterObjectArray(request: Request): Array<Any>? {
        val parameters: Array<Any>?
        val requestParameters = request.requestParameters
        if (requestParameters == null || requestParameters.isEmpty()) {
            return null
        }
        parameters = arrayOf(requestParameters.size)
        for (i in requestParameters.indices) {
            val clz = ClassManager.instance.getClassType(requestParameters[i].className)

            if (requestParameters[i].callbackObjId != null) {
                val ipcCallbackAidlInterface = callbackInterfaceMap[request.pid]
                if (ipcCallbackAidlInterface != null) {
                    parameters[i] =
                        generateCallbackProxy(clz!!, requestParameters[i].callbackObjId!!, ipcCallbackAidlInterface)
                }
            } else {
                parameters[i] = Gson().fromJson(requestParameters[i].value, clz!!)
            }

        }
        return parameters
    }

    private fun generateParameterTypeArray(requestParameterArray: Array<RequestParameter>?): Array<Class<*>>? {
        if (requestParameterArray == null) {
            return null
        }
        val classNameArray = requestParameterArray.map { it.className }.toTypedArray()
        return ClassManager.instance.getClassTypeArray(classNameArray)
    }


    private fun generateCallbackProxy(
        callbackInterface: Class<*>,
        callbackObjectId: String,
        ipcCallbackAidlInterface: IPCCallbackAidlInterface
    ): Any {
        val proxy = java.lang.reflect.Proxy.newProxyInstance(
            callbackInterface.classLoader,
            arrayOf(callbackInterface),
            object : InvocationHandler {
                override fun invoke(proxy: Any?, method: Method?, args: Array<Any>?): Any? {

                    Log.e(TAG, "Callback InvocationHandler invoke args: $args")
                    // TODO
                    val responseStr = if (args == null) {
                        sendCallbackRequest(ipcCallbackAidlInterface, callbackInterface, callbackObjectId, method)
                    } else {
                        sendCallbackRequest(
                            ipcCallbackAidlInterface,
                            callbackInterface,
                            callbackObjectId,
                            method,
                            *args
                        )
                    }
                    return if (TextUtils.isEmpty(responseStr) || TextUtils.equals(responseStr, "null")) {
                        null
                    } else {
                        val returnTypeClz = method!!.returnType
                        Gson().fromJson<Any?>(responseStr, returnTypeClz)
                    }
                }

            })
        // save object for server process gc
        // TODO 在客户端进程上的 gc
//        GCManager.instance.addRef(UUID.randomUUID().toString(), proxy) // 通过 proxyId 找到服务进程的对应obj

        return proxy
    }


    private fun sendCallbackRequest(
        ipcCallbackAidlInterface: IPCCallbackAidlInterface,
        clz: Class<*>,
        objId: String,
        method: Method?,
        vararg args: Any?
    ): String? {
        return try {
            ipcCallbackAidlInterface.callback(generateInvokeCallbackMethodRequest(objId, clz, method, *args))
        } catch (e: RemoteException) {
            e.printStackTrace()
            null
        }
    }

    private fun generateInvokeCallbackMethodRequest(
        objId: String,
        clz: Class<*>,
        method: Method?,
        vararg args: Any?
    ): String {
        val requestParameters = mutableListOf<RequestParameter>()
        if (args.isNotEmpty()) {
            args.filterNotNull()
                .forEach {
                    requestParameters.add(
                        RequestParameter(
                            it.javaClass.name,
                            Gson().toJson(it),
                            null
                        )
                    )
                }
        }

        val className = ""
        val methodName = method?.name
        val request =
            Request(
                Request.INVOKE_METHOD,
                Process.myPid(),
                objId,
                className,
                methodName,
                requestParameters.toTypedArray()
            )

        return Gson().toJson(request)
    }


}