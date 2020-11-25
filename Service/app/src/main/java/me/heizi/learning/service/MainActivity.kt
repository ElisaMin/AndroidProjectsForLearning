package me.heizi.learning.service


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import me.heizi.learning.service.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    private val binding:ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        viewClicking()
    }
    private fun viewClicking() = binding.run {
        fun checking(b:Boolean) {
            if(!b) {
                Toast.makeText(this@MainActivity,"没启动呢",Toast.LENGTH_SHORT).show()
            }
        }
        start.setOnClickListener {
//            if (!binder.pause()) {
                val editor = EditText(this@MainActivity).apply {
                    hint = "输入网址"
                }
                AlertDialog.Builder(this@MainActivity).apply {
                    setTitle("开始下载")
                    setMessage("注意 本次下载需要写入权限。")
                    setView(editor)
                    setPositiveButton("开始") { _,_->
//                        if ((ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED))
//                            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
//                        if ((ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED))
//                            binder.start(editor.text.toString(),this@MainActivity,this@MainActivity)
                    }
                }.show()
//            } else {
//                binder.start(binder.url,this@MainActivity,this@MainActivity)
//            }
        }
    }
}
