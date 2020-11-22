package me.heizi.learning.service

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import me.heizi.learning.service.databinding.ActivityMainBinding
import me.heizi.learning.service.services.DownloadingService


class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    lateinit var binder: DownloadingService.Binder
    val binding:ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    val connect = object:ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i(TAG, "onServiceConnected: binded")
            binder = (service as DownloadingService.Binder)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i(TAG, "onServiceDisconnected: called")

        }

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        viewClicking()
        Intent(this,DownloadingService::class.java).let {
            startService(it)
            bindService(it,connect,0)
        }
    }
    fun viewClicking() = binding.run {
        start.setOnClickListener {
            val editor = EditText(this@MainActivity).apply {
                hint = "输入网址"
            }
            AlertDialog.Builder(this@MainActivity).apply {
                setTitle("开始下载")
                setMessage("注意 本次下载需要写入权限。")
                setView(editor)
                setPositiveButton("开始") { _,_->

                    if ((ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED))
                        ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
                    if ((ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED))
                        binder.start(editor.text.toString(),this@MainActivity,this@MainActivity)
                }
            }.show()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, DownloadingService::class.java))
    }
}