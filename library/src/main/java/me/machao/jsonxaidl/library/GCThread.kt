package me.machao.jsonxaidl.library

import android.util.Log
import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.util.concurrent.ConcurrentHashMap

/**
 * Date  2019/2/1
 * @author charliema
 */
class GCThread(
    private val referenceQueue: ReferenceQueue<Any>,
    private val refMap: ConcurrentHashMap<PhantomReference<Any>, String>
) :
    Thread() {

    companion object {
        const val TAG = "GCThread"
    }


    override fun run() {
        super.run()
        val objIdToRemove = mutableListOf<String>()

        while (!isInterrupted) {
            Log.e(TAG, "gc start")
            synchronized(referenceQueue) {
                val objId: String?
                var reference = referenceQueue.poll()
                Log.e(TAG, "gc ref:" + reference)

                while (reference == null) {
                    // TODO 还需要发到客户端的另一种情况，callbakc的情况
                    // send to server process
                    IPCManager.instance.gc(objIdToRemove)
                    objIdToRemove.clear()

                    (referenceQueue as java.lang.Object).wait()
                    reference = referenceQueue.poll()
                }

                objId = refMap.remove(reference)
                if (objId != null) {
                    // TODO multi service in multi process

                    objIdToRemove.add(objId)
                }
            }

        }
    }
}