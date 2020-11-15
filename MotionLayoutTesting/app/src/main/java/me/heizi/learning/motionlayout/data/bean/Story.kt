package me.heizi.learning.motionlayout.data.bean

import com.google.gson.annotations.SerializedName

data class Story(
    @SerializedName("StoryName") var name:String?=null,
    @SerializedName("StoryContent") var content:String?=null
)