package me.heizi.learning.service.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import me.heizi.learning.service.R
import me.heizi.learning.service.services.DownloadingService.Statues.EMPTY_RESPONSE_BODY
import me.heizi.learning.service.services.DownloadingService.Statues.JUST_ERROR
import me.heizi.learning.service.services.DownloadingService.Statues.SUCCESS
import me.heizi.learning.service.services.DownloadingService.Statues.UNKNOWN_ERROR
import me.heizi.learning.service.services.DownloadingService.Statues.URL_NOT_ALLOW
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.RandomAccessFile
import java.net.MalformedURLException
import java.net.URL


class DownloadingService: LifecycleService() {
    object Statues {
        const val URL_NOT_ALLOW = -500
        const val EMPTY_RESPONSE_BODY = -404
        const val JUST_ERROR = -400
        const val SUCCESS = 101
        const val UNKNOWN_ERROR = -430
        const val PAUSE = -102
        const val CANCEL = -103
    }
    private var isStarted = false
    inner class Binder:android.os.Binder() {
        val liveState = MutableLiveData(0)

        lateinit var url: String
        lateinit var directory:String

        fun start(url: String, context: Context, lifecycleOwner: LifecycleOwner) {
            isStarted = true
            //观察ErrorCode
            liveState.observe(lifecycleOwner,notificationObserver)
            try {
                //测试URL是否合理
                URL(url)
            } catch (e: MalformedURLException) {
                isStarted = false
                //不合理时ERROR CODE为 URL_NOT_ALLOW
                liveState.value = URL_NOT_ALLOW
                return
            }
            //赋值
            this.url = url
            this.directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.path
            //捕获异常
            try {
                onDownloading()
            }catch (e:Exception) {
                e.printStackTrace()
                //异常
                isStarted =false
                onFailed(e.message?:"未知错误", UNKNOWN_ERROR)
            }
        }

        fun cancel():Boolean = run {
            if (isStarted) {isCanceled = true}
            isCanceled
        }
        fun pause ()= kotlin.run {
            if (isStarted) isPause = true
            isPause
        }
    }
    val binder = Binder()

    private var lengthNetwork=-1L
    private var lengthLocal = 0L
    private var file: File? = null
    private var fileWriting: RandomAccessFile?=null
    private var response: Response? = null

    var isPause = false
    var isCanceled = false

    private val client by lazy { OkHttpClient() }
    private val okhttp by lazy { client.dispatcher.executorService.asCoroutineDispatcher() }

    fun onDownloading () {
        isPause = false
        isCanceled = false
        Log.i(TAG, "onDownloading: called ${binder.directory}")
        lifecycleScope.launch(Main) {
        }
        Log.i(TAG, "onDownloading: created")
        val url = binder.url
        //异步执行
        val _response = lifecycleScope.async(okhttp) {
            client.newCall(Request.Builder().url(url).build()).execute()
        }
        //堵塞IO
        lifecycleScope.launch(Dispatchers.IO) IO@{
            Log.i(TAG, "onDownloading: blocking io")
            file = File("${binder.directory}/${binder.url.substring(binder.url.lastIndexOf('/'))}")
            val _fileWriting= lifecycleScope.async (Dispatchers.Main) {
                RandomAccessFile(file!!,"rw").also { it.seek(lengthLocal) }
            }
            lengthNetwork = _response.await().body?.contentLength() ?:-1
            if (file!!.exists()) {
                lengthLocal = file!!.length()
            }
            _response.await().close()
            Log.i(TAG, "onDownloading: closed")
            when (lengthNetwork) {
                -1L -> {
                    onFailed(reason = "网络错误", errorCode = EMPTY_RESPONSE_BODY)
                    return@IO
                }
                0L -> {
                    onFailed(reason = "下载地址为空", errorCode = EMPTY_RESPONSE_BODY)
                    return@IO
                }
                lengthLocal -> {
                    onSuccess()
                    return@IO
                }
                else -> Unit
            }
            fileWriting = _fileWriting.await()
            response = withContext (okhttp) {
                client.newCall(Request.Builder().addHeader("RANGE","byte=$lengthLocal").url(url).build()).execute()
            }


            if (response?.body == null) {
                onFailed(reason = "网络错误", errorCode = EMPTY_RESPONSE_BODY)
                return@IO
            }
            //正在写入
            Log.i(TAG, "onDownloading: writing")
            val inputStream = response!!.body!!.byteStream()
            val __file = fileWriting!!
            var byteArray = ByteArray(1024)
            var total = 0
            var len = inputStream.read(byteArray)
            Log.i(TAG, "onDownloading: writing init is done")
            while ( (len!=-1) and (!isPause) and  (!isCanceled) ) {
                total+=len
                __file.write(byteArray,0,len)
                (((total+lengthLocal)*100/lengthNetwork).toInt()).let {
                    if (it != binder.liveState.value)
                        launch (Main) { binder.liveState.value = it }
                }
                len = inputStream.read(byteArray)
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
        }
    }
    companion object {
        const val NOTIFICATION_ID = 11
        const val CHANNEL_ID = "Downloading"
        const val CHANNEL_NAME = "Downloader"
        private const val TAG = "DownloadingService"

    }
    var errorReason:String? = null

    private val notificationManager get() = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    val notificationObserver: Observer<Int> = Observer {
        Log.i(TAG, "observer: changed$it ")
        val n = NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setSmallIcon(R.drawable.ic_outline_arrow_circle_down_24)
            when (it) {
                in 0..100 ->{
                    setContentTitle("下载中")
                    setContentText("$it%")
                    setProgress(100,it,false)
                }
                URL_NOT_ALLOW ->{
                    setContentTitle("下载失败！")
                    setContentText("URL错误")
                }
                SUCCESS-> {
                    setContentTitle("下载成功")
                    setContentText("看看都下载了什么吧！")
                }
                JUST_ERROR -> {
                    setContentTitle("下载失败！")
                    setContentText("原因：$errorReason")
                }
                EMPTY_RESPONSE_BODY ->{
                    setContentTitle("网络异常")
                    setContentText("原因：$errorReason")
                }
                UNKNOWN_ERROR ->{
                    setContentTitle("下载异常")
                    setContentText("未知：$errorReason")
                }
            }
        }.build()
        notificationManager.notify(NOTIFICATION_ID,n)
    }
    //取消
    fun onCancel() {
        isCanceled = true
        onClean(true)
    }
    //暂停
    fun onPause(){
        isPause = true
    }
    fun onSuccess() = lifecycleScope.launch(Main){
        isStarted = false
        binder.liveState.value = SUCCESS
        onClean(false)
    }
    fun onFailed(reason:String?=null,errorCode:Int=Statues.JUST_ERROR) {
        isStarted = false
        errorReason=reason
        binder.liveState.value = errorCode
        onClean(true)
    }
    //清理空间
    fun onClean(byFailedOrCancel:Boolean=false) {
        response?.close()
        fileWriting?.close()
        if (byFailedOrCancel)
            if (file?.exists() == true)
                file?.delete()
    }
    override fun onBind(intent: Intent): IBinder? {
        Log.i(TAG, "onBind: binded")
        super.onBind(intent)
        onCreate()
        return binder
    }
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            notificationManager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            )
        startForeground(NOTIFICATION_ID, NotificationCompat.Builder(this@DownloadingService, CHANNEL_ID)
            .setContentTitle("准备下载")
            .setSmallIcon(R.drawable.ic_outline_arrow_circle_down_24)
            .build())
    }
}