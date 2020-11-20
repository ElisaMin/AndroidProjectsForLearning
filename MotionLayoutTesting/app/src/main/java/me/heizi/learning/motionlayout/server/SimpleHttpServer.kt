package me.heizi.learning.motionlayout.server


import android.content.Context
import android.content.res.AssetManager
import android.net.Uri
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.*
import me.heizi.learning.motionlayout.data.database.Image
import me.heizi.learning.motionlayout.data.database.MyDatabase
import java.io.File
import java.io.InputStream
import java.net.URL
import kotlin.concurrent.thread

object SimpleHttpServer {
    private const val TAG = "SimpleHttpServer"    
    private const val data_json:String =
"""[{'StoryName':'name','StoryContent':'content'},{'StoryName':'name1','StoryContent':'content2'}]"""
    private const val data_xml:String  =""


    @JvmStatic
    fun main(args: Array<String>) {
        println(args)
        URL("http://localhost:1198/data/?type=json").also {
            it.path.let(::println)
            it.query.let(::println)
        }

    }
    private lateinit var scope: CoroutineScope
    @JvmStatic
    fun start(scope: CoroutineScope,db:MyDatabase,assets: AssetManager) {
        Log.i(TAG, "start: called")
        
        this.scope = scope
        this.db = db

        if (db.size < 3) {
            Log.i(TAG, "start: inserting")
            scope.launch(Dispatchers.IO) {
                db._getDao().add(
                    Image(
                        id = 1,
                        fileName = "shadow.jpg",
                        assets.open("photos/shadow.jpg").readBytes()
                    ),Image(
                        id = 2,
                        fileName = "ugly.jpg",
                        assets.open("photos/ugly.jpg").readBytes()
                    ), Image(
                        id = 3,
                        fileName = "fakeDress.jpg",
                        assets.open("photos/fakeDress.jpg").readBytes()
                    )
                )
            }
        }

        thread(start = true) {
            Log.i(TAG, "start: starting")
            instance.start(NanoHTTPD.SOCKET_READ_TIMEOUT,false)
        }


    }
    private lateinit var db:MyDatabase
    val instance by lazy {
        object : NanoHTTPD(1198) {
            fun response(status:Response.IStatus = Response.Status.OK, mimeType:String = "text/plain",msg:String?=null):Response = newFixedLengthResponse(status,mimeType,msg)
            /* localhost:1198/data?type=xml */
            override fun serve(session: IHTTPSession): Response {
                Log.i(TAG, "serve: called")
                Log.i(TAG, "serve: the Uri is ${session.uri},parameters is ${session.parameters.toString()},splited lik this${session.uri?.split("/",ignoreCase = true)}")
                return when(session.uri?.split("/")?.get(1)) {
                    "data" -> {
                        Log.i(TAG, "serve: path is data and type is ${session.parameters["type"]?.get(0)} ")
                        when (session.parameters["type"]?.get(0)) {
                            "xml" -> response(mimeType = "text/xml", msg = data_xml)
                            "json" -> response(mimeType = "application/json", msg = data_json)
                            else -> response(Response.Status.NOT_FOUND)
                        }
                    }
                    "image" ->{
                        val id = session.parameters["id"]?.get(0)
                        if (!id.isNullOrEmpty()) {
                            val image = db find id.toInt()
                            newChunkedResponse(Response.Status.OK,"image/${image.fileName.split(".").last()}",image.bytes.inputStream())
                        }else {
                            response(Response.Status.NOT_FOUND)
                        }
                    }
                    else -> response(Response.Status.NOT_FOUND)
                }

            }
        }
    }
}
