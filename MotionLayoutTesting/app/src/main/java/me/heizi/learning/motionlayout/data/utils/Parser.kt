package me.heizi.learning.motionlayout.data.utils

import android.util.Xml
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.heizi.learning.motionlayout.data.bean.Story
import org.json.JSONArray
import org.xmlpull.v1.XmlPullParser

object Parser {
    fun parseStoriesByXML(data: String,scope:CoroutineScope)=ArrayList<Story>().apply {
         scope.launch(scope.coroutineContext+Dispatchers.IO) {
             Xml.newPullParser().let {p ->
                 p.setInput(data.reader())
                 var type = p.eventType
                 while (type!=XmlPullParser.END_DOCUMENT) {
                     var story:Story? = null
                     when(type) {
                         XmlPullParser.START_TAG ->{
                             when (p.name) {
                                 "Story" -> { story = Story()
                                 }
                                 "StoryName" -> { story?.name = p.nextText() }
                                 "StoryContent" ->{ story?.content = p.nextText() }
                             }
                         }
                         XmlPullParser.END_TAG ->{
                             if (p.name == "StoryInfo") {
                                 story?.let(::add)
                                 story = null
                             }

                         }
                     }
                    type = p.next()
                 }

             }
         }
    }
    fun parseStoriesByJSON(data: String,scope:CoroutineScope)=ArrayList<Story>().apply {
        scope.launch(scope.coroutineContext+Dispatchers.IO) {
            val jr = JSONArray(data)
            repeat(jr.length()) {
                val item = jr.optJSONObject(it)
                add(
                    Story(
                        name = item.optString("StoryName"),
                        content = item.optString("StoryContent")
                ))
            }
        }
    }
}