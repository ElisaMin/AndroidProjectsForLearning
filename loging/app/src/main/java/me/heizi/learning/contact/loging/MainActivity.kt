package me.heizi.learning.contact.loging

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.room.Room
import me.heizi.learning.contact.loging.data.rooms.MyDatabase
import me.heizi.learning.contact.loging.ui.main.MainFragment

class MainActivity : AppCompatActivity() {
    init {
        mainActivity = this

    }
    companion object {
        lateinit var mainActivity:MainActivity
        val db by lazy { Room.databaseBuilder(mainActivity,MyDatabase::class.java,"phones").allowMainThreadQueries().build() }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        mainActivity = this
    }

//    override fun onResume() {
//        super.onResume()
//        supportActionBar?.hide()
//        actionBar?.hide()
//    }
}