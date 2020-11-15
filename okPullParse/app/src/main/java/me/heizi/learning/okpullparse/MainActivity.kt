package me.heizi.learning.okpullparse

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.renderscript.ScriptGroup
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import me.heizi.learning.okpullparse.databinding.ActivityMainBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.ignoreIoExceptions
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import kotlin.concurrent.thread
import kotlin.contracts.contract
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    companion object {

        const val XML =
"""<?xml version="1.0" encoding="utf-8" ?>
<apps>
    <app>
        <id>1</id>
        <name>HEIZI Store</name>
        <version>1.0</version>
    </app>
    <app>
        <id>2</id>
        <name>ToolsStore</name>
        <version>4.3.1</version>
    </app>
    <app>
        <id>1</id>
        <name>Play Store</name>
        <version>3.0</version>
    </app>
</apps>"""
        private const val TAG = "MainActivity"
    }

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil
        setContentView(binding.root)

        onClick()
    }
    private fun onClick()= binding.run {
        gotoBaidu.setOnClickListener {
            thread {
                showMessageByDialog(getResponseBodyFromURL())
            }

        }
        parseXML.setOnClickListener {
            showMessageByDialog(pullParseXml() ?: "未知错误")
        }
    }
    private fun pullParseXml(xml: String = XML):String? {
        val parser = XmlPullParserFactory.newInstance().newPullParser().apply { setInput(xml.reader()) }
        var evenType = parser.eventType
        var id= ""
        var name=""
        var version = ""
        val result = StringBuilder()
        while (evenType!=XmlPullParser.END_DOCUMENT) {
            when(evenType) {

                XmlPullParser.START_TAG ->{
                    when(parser.name) {
                        "id" -> id = parser.nextText() ?: "undefined"
                        "name" -> name = parser.nextText() ?: "undefined"
                        "version" -> version = parser.nextText() ?: "undefined"
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "app") {
                        result.append("{id:$id,name:$name,version:$version}\n")
                        Log.d(TAG, "pullParseXml: result:$result")
                    }
                }

            }
            evenType = parser.next()
        }
        return result.takeIf { it.isNotEmpty() }?.toString()
    }

    private fun getResponseBodyFromURL(url: String = "https://www.baidu.com"):String
            = runCatching { OkHttpClient().newCall(Request.Builder().url(url).build()).execute().body?.string() ?:"结果为空" }.getOrElse { "错误:${it}" }



    private fun showMessageByDialog(message:String) = runOnUiThread {
        AlertDialog.Builder(this).setTitle("RESULT").setMessage(message).show()
    }


}

fun main() {
    fun returnAsAny(x: Any) = x
    println(returnAsAny("s") is Int)
}
//fun main() {
//    var string:String = "b"
//    fun randomThings(): Unit {
//        Random.nextInt(3).let {
//            if (it>1)
//                string = "a"
//        }
//    }
//    println(string.takeIf { it.isNotEmpty() })
//}