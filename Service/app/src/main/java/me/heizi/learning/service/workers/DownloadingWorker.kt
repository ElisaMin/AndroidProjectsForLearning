
package me.heizi.learning.service.workers

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.URL

@Suppress("BlockingMethodInNonBlockingContext")
class DownloadingWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override fun isRunInForeground(): Boolean {
        createChannel()
        return true
    }
    override suspend fun doWork(): Result {
        return try{
            downloading(
                inputData.getString(KEY_URL) ?: return Result.failure(workDataOf(KEY_REASON to "URL炸裂")),
                inputData.getString(KEY_PATH) ?: return Result.failure(workDataOf(KEY_REASON to "PATH炸裂"))
            )
        }catch (e:Exception) {
            e.printStackTrace()
            Result.failure(workDataOf(KEY_REASON to (e.message?:"空")))
        }
    }
    private val client by lazy { OkHttpClient() }
    private val okhttp by lazy { client.dispatcher.executorService.asCoroutineDispatcher() }

    private suspend fun downloading(url:String,path:String): Result {
        //testing url and notice user is failure
        val file = kotlin.runCatching { URL(url);File(path) }.getOrNull() ?: return Result.failure(
            workDataOf(KEY_REASON to "URL或PATH不合法")
        )
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
        return withContext(Dispatchers.IO) IO@{
            val fileW = RandomAccessFile(file,"rw").also { it.seek(localLength) }
            val buffer = ByteArray(1024)
            var total = 0
            val inputStream = kotlin.runCatching { _responseDownloading.await().body?.byteStream() }.getOrNull() ?: return@IO Result.failure(
                workDataOf(KEY_REASON to "内容为空")
            )
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
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
        ) }

    @SuppressLint("NewApi")
    private fun showOnForeground(progress:Int)= ForegroundInfo(0,
        (if (isSDKBiggerThenO) Notification.Builder(applicationContext, CHANNEL_ID) else Notification.Builder(applicationContext)).apply {
            setContentTitle("下载中")
            setProgress(100,progress,false)
        }.build())

    companion object {
        const val CHANNEL_NAME = "下载提示"
        const val CHANNEL_ID = "downloading"
        const val KEY_REASON = "r"
        const val KEY_URL = "u"
        const val KEY_PATH = "p"
    }
}
