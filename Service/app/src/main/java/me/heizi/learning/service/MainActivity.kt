package me.heizi.learning.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import me.heizi.learning.service.DownloadingService.Statues.EMPTY_RESPONSE_BODY
import me.heizi.learning.service.DownloadingService.Statues.SUCCESS
import me.heizi.learning.service.databinding.ActivityMainBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.wait
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.MalformedURLException
import java.net.URL
import kotlin.concurrent.thread
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    lateinit var binder: DownloadingService.Binder
    val binding:ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater).also { viewClicking() } }
    val connect = object:ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder = (service as DownloadingService.Binder)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i(TAG, "onServiceDisconnected: called")

        }

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        thread {
            Intent(this,DownloadingService::class.java).let {
                startService(it)
                bindService(it,connect,0)
            }
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
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this,DownloadingService::class.java))
    }
}


class DownloadingService:LifecycleService () {
    object Statues {
        const val URL_NOT_ALLOW = -500
        const val EMPTY_RESPONSE_BODY = -404
        const val JUST_ERROR = -400
        const val SUCCESS = 101
    }

    inner class Binder:android.os.Binder() {
        val liveState = MutableLiveData(0)
        lateinit var url: String
        lateinit var directory:String
        fun start(url: String,context: Context,lifecycleOwner: LifecycleOwner) {
            liveState.observe(lifecycleOwner,notificationObserver)
            try {
                URL(url)
            } catch (e: MalformedURLException) {
                liveState.value = Statues.URL_NOT_ALLOW
                return
            }
            this.url = url
            this.directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.path
            onDownloading()
        }
    }
    val binder = Binder()

    private var lengthNetwork=-1L
    private var lengthLocal =-1L
    private var file:File? = null
    private var fileWriting:RandomAccessFile?=null
    private var response:Response? = null

    var isPause = false
    var isCanceled = false

    private val client by lazy { OkHttpClient() }
    private val okhttp by lazy { client.dispatcher.executorService.asCoroutineDispatcher() }

    fun onDownloading () {
        startForeground(NOTIFICATION_ID,NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("准备下载")
                .build())
        val url = binder.url
        //异步执行
        val _response = lifecycleScope.async(okhttp) {
            client.newCall(Request.Builder().url(url).build()).execute()
        }
        //堵塞IO
        runBlocking (IO) {
            file = File("${binder.directory}/${binder.url.substring(binder.url.lastIndexOf('/'))}")
            lengthNetwork = _response.await().body?.contentLength() ?:-1
            if (file!!.exists()) {
                lengthLocal = file!!.length()
            }
            _response.await().close()
            when (lengthNetwork) {
                -1L -> {
                    onFailed(reason = "网络错误", errorCode = EMPTY_RESPONSE_BODY)
                    return@runBlocking
                }
                0L -> {
                    onFailed(reason = "下载地址为空", errorCode = EMPTY_RESPONSE_BODY)
                    return@runBlocking
                }
                lengthLocal -> {
                    onSuccess()
                    return@runBlocking
                }
                else -> Unit
            }
            response = withContext (okhttp) {
                client.newCall(Request.Builder().addHeader("RANGE","byte=$lengthLocal").url(url).build()).execute()
            }
            fileWriting = withContext(Main) {
                RandomAccessFile(file!!,"rw").also { it.seek(lengthLocal) }
            }

            if (response?.body == null) {
                onFailed(reason = "网络错误", errorCode = EMPTY_RESPONSE_BODY)
                return@runBlocking
            }
            //正在写入
            val inputStream = response!!.body!!.byteStream()
            val __file = fileWriting!!
            var byteArray = ByteArray(1024)
            var total = 0
            var len = inputStream.read()
            while ( (len!=-1) and (!isPause) and  (!isCanceled) ) {
                total+=len
                len = inputStream.read()
                __file.write(byteArray,0,len)
                (((total+lengthLocal)*100/lengthNetwork).toInt()).let {
                    if (it != binder.liveState.value) binder.liveState.value = it
                }
            }
            //跳转原因
            when {
                isCanceled -> {
                    onCancel()
                }
                isPause -> onPause()
                __file.length() == lengthNetwork -> onSuccess()
                else -> onFailed("未知错误")
            }
            //跳转原因


        }
    }
    companion object {
        const val NOTIFICATION_ID = 11
        const val CHANNEL_ID = "Downloading"
        const val CHANNEL_NAME = "Downloader"

    }
    private val notificationManager get() = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    var errorReason:String? = null
    val notificationObserver:Observer<Int> = Observer {
        NotificationCompat.Builder(this, CHANNEL_NAME).apply {
            when (it) {
                in 1..100 ->{
                    setContentTitle("下载中")
                    setContentText("$it%")
                    setProgress(100,it,false)
                }
                SUCCESS-> {
                    setContentTitle("下载成功")
                    setContentText("看看都下载了什么吧！")
                }
                EMPTY_RESPONSE_BODY ->{
                    setContentTitle("网络异常")
                    setContentText("原因：$errorReason")
                }
            }
        }.build()
    }
    fun onCancel() {

        onClean(true)
    }
    fun onPause(){
    }
    fun onSuccess() {
        binder.liveState.value = SUCCESS
    }
    fun onFailed(reason:String?=null,errorCode:Int=Statues.JUST_ERROR) {
        errorReason=reason
        binder.liveState.value = errorCode
        onClean(true)
    }
    fun onClean(byFailedOrCancel:Boolean=false) {
        response?.close()
        fileWriting?.close()
        if (byFailedOrCancel)
            if (file?.exists() == true)
                file?.delete()
    }
    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return binder
    }
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O)
            notificationManager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, CHANNEL_NAME,NotificationManager.IMPORTANCE_LOW)
            )
    }
}
@ObsoleteCoroutinesApi
private class Service: LifecycleService() {


    inner class Binder: android.os.Binder() {
        val liveProgress = MutableLiveData(0)
        fun startDownload(context:Context,url: String) = downloading.onStart(context, url)
    }
    companion object {
        const val NOTIFICATION_ID = 11
        const val CHANNEL_ID = "Downloading"
        const val CHANNEL_NAME = "Downloader"

    }
    private val notificationManager get() = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    private val binder = Binder()
    private val downloading by lazy {
        Downloading(
                lifecycleScope,
                onFall = { msg,e->
                    stopForeground(true)
//                    notificationManager.notify(NOTIFICATION_ID,)
                    AlertDialog.Builder(this)
                            .setTitle(msg)
                            .also { b -> e?.let { b.setMessage(it.stackTraceToString()) } }
                            .show()
                },
                onSuccess = {
                    stopForeground(true)
                },
                onProgressChanged = { binder.liveProgress.value = it }
        )
    }
    override fun onBind(intent: Intent): IBinder? { super.onBind(intent);return binder }
}
@ObsoleteCoroutinesApi
class Downloading(
        val coroutineScope: CoroutineScope,
        val onFall: ((String, Throwable?) -> Unit) = { it, e ->
            Log.e(TAG, "onFall: $it", e)
            e?.printStackTrace()
        },
        val onSuccess: () -> Unit = { Log.i(TAG, "success: success") },

        val onProgressChanged :(Int)->Unit
) {
    companion object {
//        val Network by lazy { newSingleThreadContext("Network") }
        private const val TAG = "Downloading"
    }
    private val client by lazy { OkHttpClient() }
    private val okhttp by lazy { client.dispatcher.executorService.asCoroutineDispatcher() }

    var isPause = false
    var isCanceled = false
    fun onStart(context:Context,url: String):Downloading {
        coroutineScope.launch(IO) {
            //TODO:判断url是否合法
            try {
                Uri.parse(url)!!
                //TODO:获取file
                //TODO:调用count length
                onCountingLength(url,File("${context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.path}/${url.substring(url.lastIndexOf('/'))}"))
            }catch (e:Exception) {
                onFall("URL不合法",e)
            }
        }
        return this
    }
    private fun onCountingLength(url:String,file:File) {
        //TODO 异步获取响应
        val response = coroutineScope.async(okhttp) {
            client.newCall(Request.Builder().url(url).build()).execute()
        }
        coroutineScope.launch (IO) {
            //TODO:获取本地长度 如果长度为
            val lf = file.takeIf { it.exists() }?.length() ?: 0
            //TODO:获取网络长度
            try {
                //TODO 判断响应是否成功
                if (!response.await().isSuccessful) throw IOException("响应失效")
                val ln = response.await().body?.contentLength() ?: -1
                //TODO 判断body为空则-1并且裂开 非0则下载
                when(ln) {
                    -1L -> { throw IOException("响应为空") }
                    0L->{ throw IOException("响应长度为空") }
                    //TODO 文件和网络长度一致时调用Success
                    lf ->{ onSuccess()
                        return@launch }
                    //TODO 不为0或-1时调用Downloading
                    else -> { onDownloading(url,file,lf,ln) }
                }
            }catch (e:Exception) {
                //TODO 调用失败回调
                onFall("错误：${e.message}",e)
            }finally {
                //TODO 关闭
                response.await().close()
            }
        }
    }
    private fun onDownloading(url:String,_file: File,lengthF:Long,lengthN: Long) {
        coroutineScope.launch(Default) {
            try {
                //TODO 异步创建file实例
                val file: Deferred<RandomAccessFile> = async(IO) {
                    RandomAccessFile(_file,"rw").also { it.seek(lengthF) }
                }
                //TODO 并获得创建响应
                val response = async (okhttp) {
                    client.newCall(Request.Builder().addHeader("RANGE","byte=$lengthF").url(url).build()).execute()
                }
                try {
                    //TODO 判空或失败
                    if (!response.await().isSuccessful) throw IOException("获取失败")
                    val inputStream = response.await().body!!.byteStream()
                    //TODO 写入
                    launch (IO) {
                        val __file = file.await()
                        var byteArray = ByteArray(1024)
                        var total = 0
                        var len = inputStream.read()
                        while ( (len!=-1) and (!isPause) and  (!isCanceled) ) {
                             total+=len
                            len = inputStream.read()
                            __file.write(byteArray,0,len)
                            onProgressChanged(((total+lengthF)*100/lengthN).toInt())
                        }
                        //TODO 跳转出来因为……
                        when {
                            isCanceled -> {
                                onClean(_file);onCancel()
                            }
                            isPause -> TODO("没想好")
                            else -> onSuccess()
                        }
                    }

                }catch (e:Exception) {
                    onFall("错误：${e.message}",e)
                }finally {
                    response.await().close()
                    file.await().close()
                }

            }catch (e:Exception) {
                //TODO 此时创建实例失败
                onFall("创建实例失败${e.message}",e)
            }

        }
    }
    lateinit var onCancel : ()->Unit
    private fun onClean(file: File) {
        file.deleteOnExit()
        //TODO 清内存 似乎啥也没有
    }
    public fun cancel(onCancel : ()->Unit) {
        this.onCancel

        isCanceled = true
    }
}