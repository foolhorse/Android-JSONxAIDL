package me.machao.jsonxaidl.sample

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import me.machao.jsonxaidl.library.IPCManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        IPCManager.instance.registerClass(UserManager::class.java)
        IPCManager.instance.registerClass(User::class.java)

        UserManager.getInstance().setUser(User("1112", "machao"))

        btn.setOnClickListener { doSomething() }
    }

    private fun doSomething() {

        val intent = Intent(this, RemoteActivity::class.java)
        startActivity(intent)
    }

}
