package me.heizi.learning.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import me.heizi.learning.service.databinding.ActivityMainBinding
import me.heizi.learning.service.services.DownloadingService
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.URL


class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    lateinit var binder: DownloadingService.Binder
    private val binding:ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val connect = object:ServiceConnection {

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
    private fun viewClicking() = binding.run {
        fun checking(b:Boolean) {
            if(!b) {
                Toast.makeText(this@MainActivity,"没启动呢",Toast.LENGTH_SHORT).show()
            }
        }
        pause.setOnClickListener {
            binder.pause().let(::checking)
        }
        cancel.setOnClickListener {
            binder.cancel().let(::checking)
        }
        start.setOnClickListener {
            if (!binder.pause()) {
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
            } else {
                binder.start(binder.url,this@MainActivity,this@MainActivity)
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, DownloadingService::class.java))
    }
}
class DownloadingWorker(context: Context, workerParams: WorkerParameters) :CoroutineWorker(context, workerParams) {

    override fun isRunInForeground(): Boolean {
        createChannel()
        return true
    }
    override suspend fun doWork(): Result {
        return try{
            Result.success()
        }catch (e:Exception) {
            Result.failure()
        }
    }
    val client by lazy { OkHttpClient() }
    val okhttp by lazy { client.dispatcher.executorService.asCoroutineDispatcher() }

    private suspend fun downloading(url:String,path:String):Result {
        //testing url and notice user is failure
        val file = kotlin.runCatching { URL(url);File(path) }.getOrNull() ?: return Result.failure()
        //async get length of file and network(似乎此处的async多此一举
        val _responseLength = GlobalScope.async (okhttp) {
            kotlin.runCatching {
                val it = client.newCall(Request.Builder().url(url).build()).execute()
                it.body?.contentLength().apply { it.close() }
            }.getOrNull() ?:0L
        }
        val localLength = file.run { if (exists()) length() else 0 }
        val remoteLength = _responseLength.await()
        //处理长度
        when(remoteLength) {
            in Long.MIN_VALUE..0 ->  return Result.failure(workDataOf(KEY_REASON to "网络错误或下载内容为空"))
            localLength -> return Result.success()

        }
        //get response again before download
        val _responseDownloading = GlobalScope.async(okhttp) {
            client.newCall(Request.Builder().addHeader("RANGE","byte=$localLength").url(url).build()).execute().also {
                if (it.body == null) {
                    throw IOException("内容为空!")
                }
            }
        }
        //init download objects
        return withContext(IO) IO@{
            val fileW = RandomAccessFile(file,"rw").also { it.seek(localLength) }
            val buffer = ByteArray(1024)
            var total = 0
            val inputStream = kotlin.runCatching { _responseDownloading.await().body?.byteStream() }.getOrNull() ?: return@IO Result.failure(workDataOf(KEY_REASON to "内容为空"))
            //write data that catching from response
            do {
                val len = inputStream.read(buffer)
                total+=len
                fileW.write(buffer,0,len)
                setForeground(showOnForeground((((total+localLength)*100/remoteLength).toInt())))
            }while (len!=-1)

            //result
            return@IO Result.success()
        }
    }
    private val notificationManager by lazy { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val isSDKBiggerThenO = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME,NotificationManager.IMPORTANCE_LOW)
        ) }

    @SuppressLint("NewApi")
    private fun showOnForeground(progress:Int)=ForegroundInfo(0,
        (if (isSDKBiggerThenO) Notification.Builder(applicationContext, CHANNEL_ID) else Notification.Builder(applicationContext)).apply {
            setContentTitle("下载中")
            setProgress(100,progress,false)
        }.build())

    companion object {
        const val CHANNEL_NAME = "下载提示"
        const val CHANNEL_ID = "downloading"
        const val KEY_REASON = "r"

    }
}
