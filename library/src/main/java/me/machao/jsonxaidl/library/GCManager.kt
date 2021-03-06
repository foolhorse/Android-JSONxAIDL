package me.machao.jsonxaidl.library

import android.util.Log
import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.util.ArrayList
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * Date  2019/1/31
 * @author charliema
 */
class GCManager {


    companion object {
        const val TAG = "GCManager"

        val instance: GCManager
                by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
                    GCManager()
                }
    }

    private val referenceQueue = ReferenceQueue<Any>()

    /**
     * proxy object
     */
    private val refMap = ConcurrentHashMap<PhantomReference<Any>, String>()


    private lateinit var gcThread: GCThread

    init {
        gcThread = GCThread(referenceQueue, refMap)
        startGCThread()
    }

    fun startGCThread() {
        gcThread.start()
    }

    fun stopGCThread() {
        gcThread.interrupt()
    }

    /**
     * call from client process
     */
    fun addRef(objId: String, obj: Any) {
        Log.e(TAG, "gc addRef")

        gc() // gc server process obj
        refMap.putIfAbsent(PhantomReference(obj, referenceQueue), objId)
    }

    private fun gc() {
        if (gcThread.isAlive) {
            synchronized(referenceQueue) {
                (referenceQueue as java.lang.Object).notify()
            }
        }
    }


}