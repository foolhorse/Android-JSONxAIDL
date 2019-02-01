package me.machao.jsonxaidl.sample

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_detail.*
import me.machao.jsonxaidl.library.IPCManager

/**
 * Date  2019/1/25
 * @author charliema
 */
class RemoteActivity : BaseActivity() {
    companion object {
        const val TAG = "RemoteActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        IPCManager.instance.connect(this)

        btnSingleTonInstance.setOnClickListener { btnSingleTonInstance() }
        btnNewInstance.setOnClickListener { btnNewInstance() }
        btnCallback.setOnClickListener { btnCallback() }

    }

    override fun onDestroy() {
        super.onDestroy()
        IPCManager.instance.disconnect(this)
    }

    private fun btnSingleTonInstance() {
        // 获取对象（实际上是个动态代理）
        val userManager = IPCManager.instance.getSingletonInstanceProxy(IUserManager::class.java)
        // 执行方法
        val user = userManager.getUser()

        Toast.makeText(this, "remote:" + user.name, Toast.LENGTH_SHORT).show()
    }

    private fun btnNewInstance() {
        user = IPCManager.instance.newInstanceProxy(IUser::class.java)
        user.name = "machao"
        Toast.makeText(this, "remote:" + user.name, Toast.LENGTH_SHORT).show()

//        for (i in 0 until 1000){
//            val user = IPCManager.instance.newInstanceProxy(IUser::class.java)
//        }
    }

    lateinit var user:IUser
    private fun btnCallback() {

//        user.setCallback {
//            Log.e(TAG, "user.setCallback")
//        }
        val myCallback = object:Call{
            override fun invoke() {
                Log.e(TAG, "myCallback invoke")
            }

        }
        user.setCallback(myCallback)

    }

}