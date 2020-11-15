package me.heizi.learning.motionlayout

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.createDataStore
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.map
import me.heizi.learning.motionlayout.data.bean.Story
import me.heizi.learning.motionlayout.data.database.MyDatabase
import me.heizi.learning.motionlayout.databinding.ActivityMainBinding
import me.heizi.learning.motionlayout.server.SimpleHttpServer
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity() {


    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val db:MyDatabase by lazy { Room.databaseBuilder(this,MyDatabase::class.java,"database").allowMainThreadQueries().build() }
    private val dataStore by lazy { createDataStore("main") }
    //private val networkDoing = newSingleThreadContext()actual
    private var stories:ArrayList<Story> = arrayListOf()
    fun getStories(data: String):ArrayList<Story> {
        stories = Gson().fromJson(data, object : TypeToken<ArrayList<Story>>(){}.type ) as ArrayList<Story>
        return stories
    }
    companion object {
        private const val TAG = "MainActivity"
        private const val HOST_AND_PORT = "http://localhost:1198/"
        fun String.toUrlForThis() = "$HOST_AND_PORT$this"

    }
    suspend fun getResponseBody(path: String) =
        runCatching {
            Log.i(TAG, "getResponseBody: called")
            withContext(lifecycleScope.coroutineContext+ IO) {
                Log.i(TAG, "getResponseBody: on it")
                OkHttpClient().newCall(Request.Builder().url(path.toUrlForThis()).build()).execute().body?.string()
            }
        }.getOrThrow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate: creating")
        setContentView(binding.root)
        runBlocking {
            Log.i(TAG, "onCreate: onBlock")
            launch(IO) {
                Log.i(TAG, "onCreate: starting nano http sever")
                SimpleHttpServer.start(lifecycleScope,db,resources.assets)
            }

//                Log.i(TAG, "onCreate: dataStore show time")
//                val firstTimeBootUp = preferencesKey<Boolean>("firstTimeBootUp")
//                dataStore.data.map {
//                    it[firstTimeBootUp ] ?: dataStore.edit {edit ->
//                        edit[firstTimeBootUp] = false
//                        Snackbar.make(binding.root,"向左滑动进入下一页",Snackbar.LENGTH_INDEFINITE).setAction("确定") {self->
//                            self.isVisible = false
//                        }.show()
//                    }
//                }

            initData()
        }

    }
    suspend fun initData() {

        if (getResponseBody("data?type=json")?.let(::getStories).isNullOrEmpty()) {

            Log.i(TAG, "task: empty")
            runOnUiThread {
                Log.i(TAG, "task: building dialog")
                AlertDialog.Builder(this@MainActivity).apply {
                    setMessage("网络错误，请重试")
                    
                }.show()
            }

        }else {
            showStoryOnUi()
        }

    }
    private fun showStoryOnUi(story: Story=stories[0]) = lifecycleScope.launch(Main) {
        Log.i(TAG, "showStoryOnUi: called")
        binding.titleText.text = story.name ?:"Null"
        binding.contentText.text = story.content ?: "Null"
        lifecycleScope.launch(IO) {
            Glide.with(this@MainActivity).load("image?id=${stories.indexOf(story)+1}".toUrlForThis()).submit().get()?.let {
                runOnUiThread {
                    binding.root.background = it
                }

            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        SimpleHttpServer.instance?.stop()
    }
}