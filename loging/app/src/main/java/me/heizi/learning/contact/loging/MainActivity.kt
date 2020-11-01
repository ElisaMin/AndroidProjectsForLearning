package me.heizi.learning.contact.loging

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import me.heizi.learning.contact.loging.data.rooms.MyDatabase
import me.heizi.learning.contact.loging.ui.main.MainFragment

class MainActivity : AppCompatActivity() {
    init {
        mainActivity = this

    }
    companion object {
        private const val FIRST_TIME_START = "firstTimeStart"
        lateinit var mainActivity:MainActivity
        val db by lazy { Room.databaseBuilder(mainActivity,MyDatabase::class.java,"phones").allowMainThreadQueries().build() }
        lateinit var sharedViewModel: SharedViewModel
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
         sharedViewModel = ViewModelProvider(this)[SharedViewModel::class.java]
        setPermission()
        setContentView(R.layout.main_activity)
        mainActivity = this
        sharedViewModel.isSystemContact = (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED)

    }
    infix fun checkAndAskPms(pms:String):Boolean {
        if (ContextCompat.checkSelfPermission(this,pms)!=PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(pms),1)
        }
        return ContextCompat.checkSelfPermission(this,pms)==PackageManager.PERMISSION_GRANTED
    }
    private fun setPermission():Unit = getSharedPreferences("main", MODE_PRIVATE).run {
        if(getBoolean(FIRST_TIME_START,true)) {
            //PUT
            edit().putBoolean(FIRST_TIME_START,false).apply()
            //DIALOG
            AlertDialog.Builder(mainActivity).apply {
                setTitle("权限通知")
                setMessage("如果没有授权读写权限，则保存在应用内。")
                setCancelable(false)
                setPositiveButton("授权"){self,_->
                    sharedViewModel.isSystemContact = checkAndAskPms(Manifest.permission.READ_CONTACTS)
                    checkAndAskPms(Manifest.permission.WRITE_CONTACTS)
                    findViewById<RecyclerView>(R.id.rcv_main)?.adapter?.notifyDataSetChanged()
                    self.dismiss()
                }
                setNegativeButton("取消"){self,_->
                    self.dismiss()
                    sharedViewModel.isSystemContact = false
                    findViewById<RecyclerView>(R.id.rcv_main)?.adapter?.notifyDataSetChanged()
                }
            }.show()
        }
    }


}