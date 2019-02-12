package me.machao.jsonxaidl.library

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Process
import android.os.RemoteException
import android.os.UserManager
import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import me.machao.jsonxaidl.library.model.Request
import me.machao.jsonxaidl.library.model.RequestParameter
import me.machao.jsonxaidl.library.util.log
import java.lang.reflect.InvocationHandler

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Date  2019/1/25
 *
 * @author charliema
 */
class IPCService : Service() {

    companion object {
        const val TAG = "IPCService"

        const val ACTION = "me.machao.myipc.library.action.IPCService"

        const val NEW_INSTANCE: Short = 1
        const val GET_SINGLETON_INSTANCE: Short = 2
        const val INVOKE_METHOD: Short = 3
    }

    private val callbackInterfaceMap = ConcurrentHashMap<Int, IPCCallbackAidlInterface>()


    override fun onBind(intent: Intent): IBinder? {
        return object : IPCAidlInterface.Stub() {

            @Throws(RemoteException::class)
            override fun call(requestStr: String): String? {
                try {
                    val request = Gson().fromJson(requestStr, Request::class.java)
                    Log.e(TAG, "GET_INSTANCE: request:" + request)

                    when (request.type) {
                        GET_SINGLETON_INSTANCE -> {

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
                        NEW_INSTANCE -> {
                            val parameterTypeArr = generateParameterTypeArray(request)

                            if (parameterTypeArr.any { it == null }) {
                                throw RemoteException("NEW_INSTANCE, one of Constructor Parameter Type is null")
                            }

                            val parameterTypeArrNotNull = mutableListOf<Class<*>>()
                            parameterTypeArr.forEach { parameterTypeArrNotNull.add(it!!) }

                            val constructorMethod = ClassManager.instance
                                .getConstructor(request.className, parameterTypeArrNotNull.toTypedArray())

                            if (constructorMethod == null) {
                                throw RemoteException("NEW_INSTANCE, can't find Constructor")
                            }

                            val parameterObjArr = generateParameterObjectArray(request)

                            var obj: Any? = null

                            try {
                                obj = if (parameterObjArr == null) {
                                    constructorMethod!!.newInstance()
                                } else {
                                    constructorMethod!!.newInstance(*parameterObjArr)
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
                        INVOKE_METHOD -> {
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
                    parameters[i] = generateCallbackProxy(clz!!, requestParameters[i].callbackObjId!! ,ipcCallbackAidlInterface)
                }
            } else {
                parameters[i] = Gson().fromJson(requestParameters[i].value, clz!!)
            }

        }
        return parameters
    }

    private fun generateParameterTypeArray(request: Request): Array<Class<*>?> {
        val parameterTypes: Array<Class<*>?> = arrayOf()
        val requestParameters = request.requestParameters
        if (requestParameters == null || requestParameters.isEmpty()) {
            return parameterTypes
        }
        for (i in requestParameters.indices) {
            parameterTypes[i] = ClassManager.instance.getClassType(requestParameters[i].className)
        }
        return parameterTypes
    }


    private fun generateCallbackProxy(
        callbackInterface: Class<*>,
        callbackObjectId:String,
        ipcCallbackAidlInterface: IPCCallbackAidlInterface
    ): Any {
        val proxy = Proxy.newProxyInstance(
            callbackInterface.classLoader,
            arrayOf(callbackInterface),
            object : InvocationHandler {
                override fun invoke(proxy: Any?, method: Method?, args: Array<Any>?): Any? {

                    Log.e(TAG, "Callback InvocationHandler invoke args: $args")
                    // TODO
                    val responseStr = if (args == null) {
                        sendCallbackRequest(ipcCallbackAidlInterface, callbackInterface,callbackObjectId, method)
                    } else {
                        sendCallbackRequest(ipcCallbackAidlInterface, callbackInterface,callbackObjectId, method, *args)
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
        objId:String,
        method: Method?,
        vararg args: Any?
    ): String? {
        return try {
            ipcCallbackAidlInterface.callback(generateInvokeCallbackMethodRequest(objId,clz, method, *args))
        } catch (e: RemoteException) {
            e.printStackTrace()
            null
        }
    }

    private fun generateInvokeCallbackMethodRequest(objId:String,clz: Class<*>, method: Method?, vararg args: Any?): String {
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

//        val className = clz.getAnnotation(ImplClass::class.java)!!.value
        // TODO 要不要放 className
        val className = ""
        val methodName = method?.name
        val request =
            Request(
                Process.myPid(),
                objId,
                IPCService.INVOKE_METHOD,
                className,
                methodName,
                requestParameters.toTypedArray()
            )

        return Gson().toJson(request)
    }
}
