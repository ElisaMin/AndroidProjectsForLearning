package me.heizi.learning.service

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
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

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
    fun onStart(context:Context,url: String) {
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