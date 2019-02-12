package me.machao.jsonxaidl.library

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Process
import android.os.RemoteException
import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import me.machao.jsonxaidl.library.model.Request
import me.machao.jsonxaidl.library.model.RequestParameter
import org.jetbrains.annotations.Nullable
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException

import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.*

/**
 * Date  2019/1/25
 *
 * @author charliema
 */
class IPCManager private constructor() {

    companion object {
        const val TAG = "IPCManager"

        val instance: IPCManager
                by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
                    IPCManager()
                }
    }

    private val classManager = ClassManager.instance

    private var ipcAidlInterface: IPCAidlInterface? = null

    fun registerClass(clz: Class<*>) {
        classManager.putClass(clz)
    }

    fun <T> getSingletonInstanceProxy(clz: Class<T>): T {
        val objId = UUID.randomUUID().toString()

        sendGetSingletonInstanceRequest(clz, objId)

        val proxy = generateInstanceProxy(clz, objId)
        // save object for server process gc
        GCManager.instance.addRef(objId, proxy)

        @Suppress("UNCHECKED_CAST")
        return proxy as T
    }

    fun <T> newInstanceProxy(clz: Class<T>): T {
        // 这个 object id 会用于 proxy 缓存，也就是客户端进程 gc 缓存 和服务端进程 object 缓存中。
        val objId = UUID.randomUUID().toString()

        sendNewInstanceRequest(clz, objId)

        val proxy = generateInstanceProxy(clz, objId)
        // save object for server process gc
        GCManager.instance.addRef(objId, proxy)

        @Suppress("UNCHECKED_CAST")
        return proxy as T
    }

    // TODO 调用 obj，而不是调用 clz
    private fun generateInstanceProxy(clz: Class<*>, objId: String): Any {
        val proxy = Proxy.newProxyInstance(clz.classLoader, arrayOf<Class<*>>(clz), object : InvocationHandler {
            override fun invoke(proxy: Any?, method: Method?, args: Array<Any>?): Any? {

                Log.e(TAG, "InvocationHandler invoke args: $args")
                val responseStr = if (args == null) {
                    IPCManager.instance.sendInvokeMethodRequest(clz, objId, method)
                } else {
                    IPCManager.instance.sendInvokeMethodRequest(clz, objId, method, *args)
                }
                return if (TextUtils.isEmpty(responseStr) || TextUtils.equals(responseStr, "null")) {
                    null
                } else {
                    val returnTypeClz = method!!.returnType
                    Gson().fromJson<Any?>(responseStr, returnTypeClz)
                }
            }

        })
        return proxy
    }

    private fun sendGetSingletonInstanceRequest(clz: Class<*>, objId: String): String? {
        return try {
            ipcAidlInterface!!.call(generateGetSingletonInstanceRequest(clz, objId))
        } catch (e: RemoteException) {
            e.printStackTrace()
            null
        }
    }


    private fun sendNewInstanceRequest(clz: Class<*>, objId: String): String? {
        return try {
            ipcAidlInterface!!.call(generateNewInstanceRequest(clz, objId))
        } catch (e: RemoteException) {
            e.printStackTrace()
            null
        }
    }


    private fun sendInvokeMethodRequest(clz: Class<*>, objId: String, method: Method?, vararg args: Any?): String? {
        return try {
            ipcAidlInterface!!.call(generateInvokeMethodRequest(clz, objId, method, *args))
        } catch (e: RemoteException) {
            e.printStackTrace()
            null
        }
    }

    fun gc(objIdList: List<String>) {
        try {
            ipcAidlInterface!!.gc(objIdList)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun generateGetSingletonInstanceRequest(clz: Class<*>, objId: String): String {

        val className = clz.getAnnotation(ImplClass::class.java)!!.value
        val request = Request(Process.myPid(), objId, IPCService.GET_SINGLETON_INSTANCE, className, null, null)
        return Gson().toJson(request)
    }

    private fun generateNewInstanceRequest(clz: Class<*>, objId: String): String {
        val className = clz.getAnnotation(ImplClass::class.java)!!.value
        val request = Request(Process.myPid(), objId, IPCService.NEW_INSTANCE, className, null, null)
        return Gson().toJson(request)
    }

    private fun generateInvokeMethodRequest(clz: Class<*>, objId: String, method: Method?, vararg args: Any?): String {
        val requestParameters = mutableListOf<RequestParameter>()
        if (args.isNotEmpty()) {
            args.filterNotNull()
                .forEachIndexed { index, it ->
                    if (method != null) {
                        val clz = method.parameterTypes[index]
                        // TODO 如果参数的类型是 接口（或者注解标明？），那么作为回调处理，那么这里不需要传入参数对象
                        // 而是把这个参数对象放进 缓存中，等待服务端进程发送 Binder 调用过来时，找到这个对象并调用。
                        // 在服务端进程中，如果发现参数的类型是 回调，那么服务端进程就地创建个动态代理。
                        if (clz.isInterface) {

                            val callbackObjId = UUID.randomUUID().toString()
                            ClassManager.instance.putObject(callbackObjId, it)
                            requestParameters.add(
                                RequestParameter(
                                    //TODO
                                    method.parameterTypes[index].name,
                                    null,
                                    callbackObjId
                                )
                            )
                        } else {
                            requestParameters.add(
                                RequestParameter(
                                    //TODO
//                                    method.parameterTypes[index],
                                    it.javaClass.name,
                                    Gson().toJson(it),
                                    null
                                )
                            )
                        }
                    }
                }
        }


        val className = clz.getAnnotation(ImplClass::class.java)!!.value
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

    private val ipcCallbackAidlInterface = object : IPCCallbackAidlInterface.Stub() {
        override fun callback(response: String?): String? {
            Log.e(TAG, "callback in caller process:$response")
            val request = Gson().fromJson(response, Request::class.java)

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


    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            ipcAidlInterface = IPCAidlInterface.Stub.asInterface(service)
            (ipcAidlInterface as IPCAidlInterface).setCallbackIInterface(
                Process.myPid(),
                ipcCallbackAidlInterface.asBinder()
            )
        }

        override fun onServiceDisconnected(name: ComponentName) {

        }
    }

    fun disconnect(context: Context) {
        context.unbindService(serviceConnection)
    }

    @JvmOverloads
    fun connect(context: Context, packageName: String? = null) {
        bind(context, packageName, IPCService::class.java)
    }

    private fun bind(context: Context, packageName: String?, serviceClz: Class<out IPCService>) {
        val i = Intent()
        if (TextUtils.isEmpty(packageName)) {
            i.setClass(context, serviceClz)
        } else {
            i.setPackage(packageName)
            i.action = IPCService.ACTION
        }
        context.bindService(
            i,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )

    }


}
