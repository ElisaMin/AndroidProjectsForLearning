package me.heizi.learning.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.wait
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.URL

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }
}



class DownloadingService:LifecycleService () {

    class Binder:android.os.Binder() {
        fun start(url: String) {
            URL(url)
        }
    }

    override fun onCreate() {
        super.onCreate()
    }
}
@ObsoleteCoroutinesApi
class Service: LifecycleService() {


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
    fun createNewNotification(
            title:String,
            showProgress:Boolean = true
    ) = NotificationCompat.Builder(this, CHANNEL_ID).apply {
        if (showProgress)
            binder.liveProgress.observe(this@Service) {

            }
    }
    override fun onBind(intent: Intent): IBinder? { super.onBind(intent);return binder }
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O)
            notificationManager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, CHANNEL_NAME,NotificationManager.IMPORTANCE_LOW)
            )
    }



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