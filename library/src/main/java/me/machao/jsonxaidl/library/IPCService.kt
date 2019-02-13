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

    }

    override fun onBind(intent: Intent): IBinder? {
        return ServerBinder()
    }

}
